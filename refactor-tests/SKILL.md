---
name: refactor-tests
description: Refactor and modernize Java test suites for readability, maintainability, and modern idioms. Use when the user asks to clean up tests, refactor tests, improve test readability, modernize test code, add assertion descriptions, reduce test duplication, apply test best practices, or run test cleanup on a codebase. Targets Java 17+ with JUnit 5/6 and AssertJ. Do not use for writing new tests from scratch or for non-Java projects.
---

# Refactor Tests

Analyze and refactor Java test code for readability, maintainability, and modern
best practices. Inspired by Ted M. Young's (JitterTed) refactoring tests
guidelines, combined with OpenRewrite automation and modern Java idioms.

## Guiding Philosophy

Tests are living documentation. They tell other developers how to use the code
and what behavior to expect. Every refactoring decision should optimize for
**readability in isolation** - a developer should understand any single test
without looking elsewhere.

Prefer verbosity over hidden setup. Prefer explicit over clever. Prefer domain
language over technical jargon.

## Phase 1: Scope and Configuration

Step 1: Determine scope. Ask the user whether to target:
  - A single test file (user provides the path)
  - All test files in the codebase

Step 2: Identify excluded test tags. Scan for `@Tag` annotations used across
test files. Present the user with a summary:

> "Before refactoring, I need to run all tests to confirm we are green. I found
> these tags in your test suite: `@Tag("slow")`, `@Tag("costly")`,
> `@Tag("research")`. I will exclude tests with these tags: [slow, costly].
> Want to adjust this list before I proceed?"

Wait for user confirmation before proceeding.

Step 3: Detect the build tool (Maven or Gradle) and test framework version
(JUnit 4, JUnit 5, or JUnit 6). Record these for later steps.

## Phase 2: Go Green

Step 4: Run the full test suite (excluding confirmed tags) with quiet output.

For Maven:
```bash
mvn -q test -Dgroups='!slow & !costly'
```

For Gradle:
```bash
./gradlew -q test --tests '*' -PexcludeTags=slow,costly
```

Step 5: If any tests fail, stop and report failures to the user. Do not proceed with refactoring until all included tests pass. Refactoring from red is unsafe.

## Phase 3: OpenRewrite Automated Cleanup

Step 6: Before any manual refactoring, check whether OpenRewrite can handle mechanical cleanups. Read `references/openrewrite-recipes.md` for the full recipe list and instructions.

Present the user with applicable recipes based on what was detected in Phase 1. Once the user approves, run the selected recipes with suppressed output:

```bash
mvn -q rewrite:run \
  --define rewrite.activeRecipes=<SELECTED_RECIPES> \
  --define rewrite.recipeChangeLogLevel=ERROR
```

- `-q` suppresses Maven lifecycle noise.
- `recipeChangeLogLevel=ERROR` suppresses per-file change warnings.
- On success, output is empty or near-empty (token-safe).
- On failure, errors are still printed for diagnosis.

After OpenRewrite completes, re-run the test suite with quiet output:

```bash
mvn -q test
```

Confirm green before proceeding to Phase 4.

## Phase 4: Automatic Refactorings

Apply these refactorings without asking for confirmation. They are safe and universally beneficial.

### 4a: Add `.as()` Descriptions to All Assertions

Scan every assertion call (`assertThat`, `assertTrue`, `assertFalse`, `assertEquals`, `assertNotNull`, etc.). If an assertion lacks an `.as()` description, add one that describes the expected behavior in domain terms.

Use `.as("description")` - never `@Description`.

**Before:**
```java
assertThat(ensemble.isCompleted()).isTrue();
```

**After:**
```java
assertThat(ensemble.isCompleted())
    .as("Ensemble should be completed after markComplete is called")
    .isTrue();
```

For boolean assertions, prefer AssertJ's domain-specific methods when possible:
```java
// Before
assertThat(result).isEqualTo(true);
// After  
assertThat(result)
    .as("Operation should succeed")
    .isTrue();
```

### 4b: Ensure SCA Structure with Blank Line Separation

Each test method should have visible separation between Setup, Command (execute), and Assert sections. Add blank lines between sections if missing.

### 4c: Simplify throws Clauses

If test methods declare `throws Exception` or `throws` checked exceptions that are not actually thrown, simplify or remove them.

## Phase 5: Analysis and Proposals

Read each test file and identify refactoring opportunities. Classify them and present to the user in two groups.

### Group A: Readability Quick Wins

For each finding, show the file, line, current code, and proposed change.

1. **Unnamed magic values** - Constructor arguments or method parameters whose meaning is unclear. Propose extracting to a named local variable.
```java
// Before
new Ensemble(ZonedDateTime.now())
// After
ZonedDateTime ensembleStartDateTime = ZonedDateTime.now();
new Ensemble(ensembleStartDateTime)
```

2. **Irrelevant data not marked as dummy** - Parameters that do not affect the test outcome but are required by the constructor. Propose using a named constant or variable like `IRRELEVANT_SUIT` or `DUMMY_NAME`.

3. **Hidden setup in @BeforeEach** - If @BeforeEach creates objects that are then asserted against in ways that require knowing the setup details, propose inlining that setup into the test methods that need it.

4. **Excessive parameterized test cases** - If parameterized tests have entries that do not exercise distinct boundary conditions, propose removing redundant entries and documenting why the remaining ones matter.

### Group B: Structural Refactorings

Read `references/abstraction-ladder.md` for detailed guidance on when to apply each level. For each finding, explain the trade-off and ask for approval.

1. **Extract factory methods** - When 2-3 tests create similar objects with small variations, propose a static factory method with parameters for only the parts that vary.

2. **Create test builders** - When 4+ variations of an object exist across tests, propose a test-specific builder class with reasonable defaults. Builders belong in test code, not production code.

3. **Create customizers** - When object construction involves complex graphs with child objects (e.g., event sourcing scenarios where creating a parent entity also creates child events), propose the customizer pattern. Read `references/abstraction-ladder.md` for the customizer pattern details.

4. **Create custom assertions** - When tests repeatedly check the same combination of properties on a domain object, propose a custom AssertJ assertion class. Read `references/custom-assertions.md` for the template.

5. **Extract to _Factory class** - When factory methods are needed across multiple test classes, propose moving them to a shared factory class.

### Group C: Naming & Organization Proposals

Identify opportunities to improve test clarity through better naming and structural organization using `@Nested` classes.

**Naming Convention (following Ted M. Young / JitterTed):**
- Use plain camelCase behavioral descriptions, NOT given_when_then.
- Test name should read as a declarative statement of behavior.
- Pattern: `<subject><verb><expectedOutcome>` (e.g., `ensembleAcceptsMember`).
- NEVER include production method names in test names (fragile to refactoring).
- Focus on WHAT behavior is verified, not HOW.

**Anti-patterns to flag:**
- `given_when_then` underscored names -> propose camelCase behavioral name.
- Method names embedded in test names (e.g., `testGetUser`) -> propose behavioral name.
- `test` prefix (JUnit 3 relic) -> propose removal.
- Vague names (e.g., `testHappyPath`, `testEdgeCase`) -> propose specific behavior.

**Organization:**
- Propose `@Nested` class groupings where tests naturally cluster by concept or shared state.
- Nested class names should be noun phrases (e.g., `CommandsGenerateEvents`, `EventsProjectState`).

**Output format:** Present as a table:
| Current Name | Proposed Name | Proposed @Nested Group |
|---|---|---|

### Efficiency Heuristic

Before proposing any multi-file rename or repetitive transformation, check whether IntelliJ IDEA's refactoring tools can do it faster. If so, provide the exact IDE steps instead of making the changes manually:

> "This rename affects 47 usages across 12 files. Instead of editing each one, use IntelliJ: place cursor on the symbol -> Shift+F6 -> type new name -> Enter."

## Phase 6: Execute Approved Refactorings

Step 7: Apply only the refactorings the user approved. After each batch of changes (per file or per refactoring type), re-run the test suite to confirm green. If any test breaks, revert the last change and investigate.

## Phase 7: Reflection and Self-Improvement

Step 8: After completing all refactorings, write a reflection entry to `journal/reflection-log.md` (create the file if it does not exist). The entry should include:
```markdown
## [DATE] - [PROJECT NAME]

### Scope
- Files analyzed: N
- Files modified: N

### Refactorings Applied
- [List each type and count]

### Patterns Observed
- [Recurring smells or project-specific idioms noticed]

### Skill Improvement Notes
- [Any heuristic that was unclear, wrong, or missing]
- [Any new pattern worth adding to the skill]
- [Any OpenRewrite recipe that would have helped but was not listed]
```

Step 9: Review the reflection. If any "Skill Improvement Notes" suggest a concrete change to this skill's instructions or references, propose the change to the user. If approved, apply the edit to the relevant file in this skill directory.

## Modern Java Considerations

When refactoring tests targeting Java 17+, also look for:

- **Records for test data**: If a test creates a class solely to hold test data with equals/hashCode, suggest converting to a Java record.
- **Pattern matching in assertions**: If custom assertion code uses chains of `instanceof` checks and casts, refactor to use pattern matching:
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

- **Sealed types**: If domain types are sealed interfaces, custom assertions can leverage exhaustive switch expressions for completeness.
- **JUnit 6 features** (if on JUnit 6): Suggest `@ParameterizedClass` when an entire test class needs to run against multiple configurations. Note that JUnit 6 uses FastCSV for `@CsvSource` (stricter parsing). Suggest removing JRE-based conditional annotations for versions below 17.
