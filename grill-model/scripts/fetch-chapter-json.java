// fetch-chapter-json.java — fetch one prooph board chapter's JSON to disk.
//
// The completeness gate's element-ref audit needs the raw get_chapter JSON as a file, but some
// hosts (DSH's proophboard MCP, observed 2026-09-24) return it inline and never spool one. This
// produces the same payload straight from the board API, so the audit always has its input:
//
//     java fetch-chapter-json.java <chapterId> <outPath>
//
// Auth: PROOPHBOARD_API_KEY from the repo's `.proophboard/.env.local` (cwd-relative; that file
// wins over the process environment — the tier_b_contract convention), else the process env.
// Exit 0 on success; 2 usage; 1 on a missing key or any HTTP >= 400.
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FetchChapterJson {

    private static final String BASE_URL = "https://flow.prooph-board.com/api";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: java fetch-chapter-json.java <chapterId> <outPath>");
            System.exit(2);
        }
        Map<String, String> env = new HashMap<>(System.getenv());
        Path dotEnv = Path.of(".proophboard", ".env.local");
        if (Files.exists(dotEnv)) {
            for (String line : Files.readAllLines(dotEnv, StandardCharsets.UTF_8)) {
                String t = line.strip();
                if (t.isEmpty() || t.startsWith("#") || !t.contains("=")) continue;
                int i = t.indexOf('=');
                env.putIfAbsent(t.substring(0, i).strip(),
                        t.substring(i + 1).strip().replaceAll("^\"|\"$", ""));
            }
        }
        String key = env.get("PROOPHBOARD_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("PROOPHBOARD_API_KEY not set; add it to .proophboard/.env.local"
                    + " or the process environment");
            System.exit(1);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/chapters/" + args[0]))
                .header("Authorization", "Bearer " + key)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            System.err.println("HTTP " + response.statusCode() + " fetching chapter " + args[0]
                    + " — body: " + response.body()
                    .substring(0, Math.min(300, response.body().length())));
            System.exit(1);
        }
        Files.writeString(Path.of(args[1]), response.body(), StandardCharsets.UTF_8);
        System.out.println("wrote " + args[1] + " (" + response.body().length() + " chars)");
    }
}
