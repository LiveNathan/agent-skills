# Java: tooling and idioms

Targets Java 17+ with JUnit 5/6 and AssertJ. Companion references:
`abstraction-ladder-java.md`, `custom-assertions.md`, `openrewrite-recipes.md`.

## Phase 1: detection

**Exclusions.** Scan for `@Tag` annotations across test files and present them:

> "Before refactoring, I need to run all tests to confirm we are green. I found these tags in your
> test suite: `@Tag("slow")`, `@Tag("costly")`, `@Tag("research")`. I will exclude tests with these
> tags: [slow, costly]. Want to adjust this list before I proceed?"

**Environment.** Detect the build tool (Maven or Gradle) and the framework version (JUnit 4, 5,
or 6).

## Phase 2: baseline command

```bash
mvn -q test -Dgroups='!slow & !costly'          # Maven
./gradlew -q test --tests '*' -PexcludeTags=slow,costly   # Gradle
```

## Phase 3: OpenRewrite

Read `openrewrite-recipes.md` for the recipe list. Present the applicable recipes based on what
Phase 1 detected; once approved:

```bash
mvn -q rewrite:run \
  --define rewrite.activeRecipes=<SELECTED_RECIPES> \
  --define rewrite.recipeChangeLogLevel=ERROR
```

`-q` suppresses lifecycle noise and `recipeChangeLogLevel=ERROR` suppresses per-file change
warnings, so success is near-silent while failures still print. Then re-run `mvn -q test`.

## Phase 4: automatic refactorings

**Add `.as()` descriptions to all assertions.** Scan every `assertThat`, `assertTrue`, `assertEquals`
etc. If it lacks an `.as()`, add one describing the expected behavior in domain terms. Use
`.as("...")`, never `@Description`.

```java
// Before
assertThat(ensemble.isCompleted()).isTrue();
// After
assertThat(ensemble.isCompleted())
    .as("Ensemble should be completed after markComplete is called")
    .isTrue();
```

Prefer AssertJ's domain-specific methods over equality on booleans: `assertThat(result).isTrue()`,
not `.isEqualTo(true)`.

**Simplify throws clauses.** Remove `throws Exception` and other checked exceptions that the test
does not actually throw.

## Phase 5: naming and organization

Plain camelCase behavioral descriptions, **not** `given_when_then`. Pattern:
`<subject><verb><expectedOutcome>` — e.g. `ensembleAcceptsMember`.

Group with `@Nested` classes named as noun phrases: `CommandsGenerateEvents`, `EventsProjectState`.

## Group D: modern Java opportunities

- **Records for test data** — a class that exists only to hold test data with equals/hashCode
  becomes a record.
- **Pattern matching in assertions** — chains of `instanceof` + cast become record patterns:

```java
// Before
if (actual instanceof RichResult.Failure) {
    String msg = ((RichResult.Failure<T>) actual).errorMessage();
    failWithMessage("...", msg);
}
// After (Java 17+)
if (actual instanceof RichResult.Failure<T>(String errorMessage)) {
    failWithMessage("...", errorMessage);
}
```

- **Sealed types** — custom assertions over a sealed interface can use exhaustive switch expressions.
- **JUnit 6** — suggest `@ParameterizedClass` when a whole class should run against multiple
  configurations. JUnit 6 uses FastCSV for `@CsvSource` (stricter parsing), and JRE-conditional
  annotations for versions below 17 can go.
