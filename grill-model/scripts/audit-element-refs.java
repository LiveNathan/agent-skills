// audit-element-refs.java — Java 26 port of audit-element-refs.py
//
// Check that every `:::element` reference in a chapter's slice details resolves.
//
// Scenario references name elements by string. Nothing on the board validates them, so a rename,
// a typo, or a reference to an element that was never created leaves a dangling pointer that reads
// as authoritative — and sends a test-writer hunting for a sticky that does not exist.
//
// Usage:
//     java audit-element-refs.java <get_chapter-json>    # the file get_chapter spooled to disk
//     get_chapter ... | java audit-element-refs.java -   # or piped
//
// Exit 0 = every reference resolves. Exit 1 = at least one does not.
//
// DANGLING is always a failure: the name matches no element anywhere in the chapter.
//
// CROSS-SLICE is reported but not failed: the name matches an element in a *different* slice. Often
// legitimate — a Given event authored by an earlier slice — but it is also how a foreign event from
// another chapter sneaks in. Events owned by another chapter should be plain YAML with an
// attribution line, not `:::element`: adding a sticky for them would make a read slice mixed-type
// and fork an element identity that chapter owns.
//
// Port note: the Python original checked group 2 (the description line) against element names, so
// every non-empty description read as DANGLING. This port checks group 1 — the element name — which
// is what the docstring and the completeness gate have always described.
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AuditElementRefs {

    // `:::element <name>` on one line, then the description line. Python's \w is Unicode-aware,
    // so use [\p{L}\p{N}_] rather than the ASCII-only Java \w.
    private static final Pattern REF = Pattern.compile(
            ":::element[ \\t]+([\\p{L}\\p{N}_]+)[ \\t]*\\n[ \\t]*([^\\n]+)");

    private static final String USAGE = """
            Usage:
                java audit-element-refs.java <get_chapter-json>   # the file get_chapter spooled to disk
                get_chapter ... | java audit-element-refs.java -  # or piped
            """;

    public static void main(String[] args) throws IOException {
        System.exit(run(args));
    }

    static int run(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println(USAGE.strip());
            return 2;
        }

        String text = args[0].equals("-")
                ? new String(System.in.readAllBytes(), StandardCharsets.UTF_8)
                : Files.readString(Path.of(args[0]));

        Map<String, Object> chapter = chapter(text);
        Map<String, Set<String>> bySlice = new HashMap<>();
        Set<String> allNames = new HashSet<>();
        for (Object eo : list(chapter.get("elements"))) {
            Map<String, Object> e = obj(eo);
            String sliceId = str(e.get("sliceId"));
            String name = str(e.get("name"));
            bySlice.computeIfAbsent(sliceId, k -> new HashSet<>()).add(name);
            allNames.add(name);
        }

        List<Map<String, Object>> slices = new ArrayList<>();
        for (Object so : list(chapter.get("slices"))) {
            slices.add(obj(so));
        }
        slices.sort(Comparator.comparingInt(s -> intVal(s.get("index"))));

        int failed = 0;
        int warned = 0;
        for (Map<String, Object> s : slices) {
            Set<String> namesHere = bySlice.getOrDefault(s.get("id"), Set.of());
            Set<String> dangling = new TreeSet<>();
            Set<String> cross = new TreeSet<>();
            String details = str(s.get("details"));
            Matcher m = REF.matcher(details == null ? "" : details);
            while (m.find()) {
                String n = m.group(1);
                if (!allNames.contains(n)) {
                    dangling.add(n);
                } else if (!namesHere.contains(n)) {
                    cross.add(n);
                }
            }
            if (!dangling.isEmpty() || !cross.isEmpty()) {
                System.out.println("[" + s.get("index") + "] " + s.get("label"));
            }
            if (!dangling.isEmpty()) {
                failed++;
                System.out.println("    DANGLING — matches no element in this chapter: " + dangling);
            }
            if (!cross.isEmpty()) {
                warned++;
                System.out.println("    cross-slice — defined in another slice: " + cross);
            }
        }

        int n = slices.size();
        if (failed > 0) {
            System.out.printf("%nFAIL — %d slice(s) with dangling references across %d slices%n", failed, n);
            return 1;
        }
        System.out.print("PASS — all :::element references resolve across " + n + " slices");
        if (warned > 0) {
            System.out.print(" (" + warned + " slice(s) with cross-slice references, review each)");
        }
        System.out.println();
        return 0;
    }

    // get_chapter spools as [{"type": "text", "text": "<json>"}]; accept the bare object too.
    private static Map<String, Object> chapter(String text) {
        Object root = Json.parse(text);
        if (root instanceof List<?> list) {
            String inner = (String) obj(list.get(0)).get("text");
            return obj(Json.parse(inner));
        }
        return obj(root);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> obj(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
        return (List<Object>) o;
    }

    private static String str(Object o) {
        return o == null ? null : (String) o;
    }

    private static int intVal(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}

// Minimal recursive-descent JSON parser, dependency-free so the script keeps the single-file
// launch contract. Only the shapes get_chapter actually produces are exercised, but the parser is
// a general JSON parser because hand-trimming it to "what the board emits today" is how a rename
// in the board's API becomes a silent script bug.
final class Json {
    private Json() {}

    static Object parse(String text) {
        P p = new P(text);
        Object v = p.value();
        p.ws();
        if (!p.atEnd()) throw p.error("trailing content");
        return v;
    }

    private static final class P {
        private final String s;
        private int i;

        P(String s) {
            this.s = s;
        }

        private Object value() {
            ws();
            if (atEnd()) throw error("expected a value");
            return switch (peek()) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> { literal("true"); yield Boolean.TRUE; }
                case 'f' -> { literal("false"); yield Boolean.FALSE; }
                case 'n' -> { literal("null"); yield null; }
                default -> number();
            };
        }

        private Map<String, Object> object() {
            expect('{');
            Map<String, Object> m = new LinkedHashMap<>();
            ws();
            if (peek() == '}') {
                i++;
                return m;
            }
            while (true) {
                ws();
                String k = string();
                ws();
                expect(':');
                m.put(k, value());
                ws();
                if (peek() == ',') {
                    i++;
                } else {
                    expect('}');
                    return m;
                }
            }
        }

        private List<Object> array() {
            expect('[');
            List<Object> l = new ArrayList<>();
            ws();
            if (peek() == ']') {
                i++;
                return l;
            }
            while (true) {
                l.add(value());
                ws();
                if (peek() == ',') {
                    i++;
                } else {
                    expect(']');
                    return l;
                }
            }
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) throw error("unterminated string");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (atEnd()) throw error("unterminated escape");
                char e = s.charAt(i++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw error("short \\u escape");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw error("bad escape \\" + e);
                }
            }
        }

        private Object number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String num = s.substring(start, i);
            if (num.isEmpty()) throw error("expected a value");
            try {
                // Keep the boxed type. A ternary of Double.valueOf vs Long.valueOf would be a
                // numeric conditional (JLS 15.25) — both operands are numeric-convertible, so the
                // expression type would be double and the Long branch widened to 0.0. An if/else
                // preserves each branch's own boxed type.
                if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                    return Double.valueOf(num);
                }
                return Long.valueOf(num);
            } catch (NumberFormatException e) {
                return Double.valueOf(num);
            }
        }

        private void literal(String lit) {
            if (!s.startsWith(lit, i)) throw error("expected " + lit);
            i += lit.length();
        }

        private void expect(char c) {
            if (atEnd() || s.charAt(i) != c) throw error("expected '" + c + "'");
            i++;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private boolean atEnd() {
            return i >= s.length();
        }

        private char peek() {
            return s.charAt(i);
        }

        private IllegalArgumentException error(String msg) {
            return new IllegalArgumentException(msg + " at offset " + i);
        }
    }
}
