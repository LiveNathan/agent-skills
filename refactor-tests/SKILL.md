---
name: refactor-tests
description: Refactor and modernize an existing test suite for readability, maintainability, and modern idioms. Use when the user asks to clean up tests, refactor tests, improve test readability, modernize test code, add assertion descriptions, reduce test duplication, or run test cleanup on a codebase. Covers Java (JUnit/AssertJ) and TypeScript (Jest/RTL/Playwright). Do not use for writing new tests from scratch.
---

# Refactor Tests

Analyze and refactor existing test code for readability, maintainability, and modern best
practices. Inspired by Ted M. Young's (JitterTed) refactoring tests guidelines and Kent C. Dodds'
Testing Library philosophy.

**Detect the language first, then read the matching reference and use it throughout.** The phases
below are shared; the tooling, commands, and idioms live in the reference:

- Java (Maven/Gradle, JUnit 5/6, AssertJ) → `references/java.md`
- TypeScript (Jest, React Testing Library, Playwright) → `references/typescript.md`

## Guiding Philosophy

- **Tests are living documentation.** They tell other developers how to use the code and what
  behavior to expect.
- **Readability in isolation.** A developer should understand any single test without looking
  elsewhere. Prefer verbosity over hidden setup, explicit over clever, domain language over
  technical jargon.
- **Test behavior, not implementation.** Test what a user of the code can observe.
- **Respect existing architectural patterns.** Infrastructure Wrappers, Nullables, and
  OutputTracker are deliberate — do not replace them with conventional mocks unless asked.

**Scope boundary:** this skill assumes test *coverage* is already correct. It does not add tests for
missing failure paths, audit `createNull` defaults, or evaluate whether the right behaviors are
tested. Those belong to `paranoic-telemetry`, which runs before this one. If you notice an obvious
coverage gap, flag it and recommend `paranoic-telemetry` — do not write the test yourself.

## Phase 1: Scope and Configuration

**Step 1:** Determine scope — a single test file (user provides the path) or all test files.

**Step 2:** Identify what to exclude from the baseline run, per the language reference (Java: `@Tag`
annotations; TypeScript: the integration and E2E categories). Present a summary of what you found
and what you plan to exclude, then **wait for confirmation**.

**Step 3:** Detect the build tool / package manager and test framework version, per the language
reference. Record these for later steps.

## Phase 2: Go Green

**Step 4:** Run the test suite (minus the confirmed exclusions) with quiet output, using the command
from the language reference.

**Step 5:** If any test fails, stop and report. **Refactoring from red is unsafe** — do not proceed
until the included tests pass.

## Phase 3: Automated Cleanup

**Step 6:** Before any manual refactoring, let the tooling do the mechanical work — OpenRewrite
(Java) or ESLint auto-fix (TypeScript). The language reference lists the recipes/plugins and the
exact commands.

**Skip this entire phase when the scope is a single test file.** These tools operate suite-wide;
running them for a one-file refactor produces noisy diffs in unrelated files. Note the skip to the
user and go to Phase 4.

Re-run the suite and confirm green before Phase 4.

## Phase 4: Automatic Refactorings

Apply these without asking. They are safe and universally beneficial.

**Ensure AAA structure with blank-line separation.** Each test should visibly separate Arrange, Act,
and Assert. Add the blank lines if missing.

Then apply every item under "Automatic refactorings" in the language reference (Java: `.as()`
descriptions, throws-clause cleanup; TypeScript: `screen` queries, `userEvent`, query variants,
jest-dom matchers, `act()` and `waitFor` fixes).

## Phase 5: Analysis and Proposals

Read each test file, identify opportunities, and present them to the user in groups. For each
finding show the file, line, current code, and proposed change.

### Group A: Readability Quick Wins

1. **Unnamed magic values** — constructor or method arguments whose meaning is unclear. Extract to a
   named local variable or constant.
2. **Irrelevant data not marked as dummy** — parameters that don't affect the outcome but are
   required by the constructor. Use a named constant like `IRRELEVANT_SUIT`, or a factory default.
3. **Hidden setup in `@BeforeEach` / `beforeEach`** — if setup creates objects whose details a test
   must know in order to read its assertions, inline that setup into the tests that need it.
4. **Excessive parameterized cases** — entries that don't exercise a distinct boundary condition.
   Remove them and document why the survivors matter.
5. **Unclear `createNull` defaults** — when the *inconvenience* of a default isn't self-documenting,
   name it. Do not change the value — that is a coverage decision owned by `paranoic-telemetry` —
   just make it readable.

```java
// Before
createNull(ZoneId.of("Australia/Lord_Howe"))
// After
static final ZoneId INCONVENIENT_DEFAULT_ZONE = ZoneId.of("Australia/Lord_Howe");
createNull(INCONVENIENT_DEFAULT_ZONE)
```

### Group B: Structural Refactorings

Read the abstraction ladder named in the language reference for when to apply each rung. Explain the
trade-off and ask for approval on each.

1. **Extract factory methods** — 2–3 tests building similar objects with small variations.
   Parameterize only what varies.
2. **Create test builders** — 4+ variations of an object across tests. Builders live in test code,
   never in production code.
3. **Create customizers** — complex object graphs with child objects (e.g. event-sourcing scenarios
   where creating a parent entity also creates child events).
4. **Create custom assertions/matchers** — when tests repeatedly check the same combination of
   properties on a domain object. Template in the language reference.
5. **Promote to a shared factory** — when factories are needed across multiple test files.

### Group C: Naming & Organization

Test names read as declarative statements of behavior. Focus on WHAT is verified, not HOW, and keep
production method names out of the name — they make tests fragile to renaming.

**Anti-patterns to flag:**

- `given_when_then` underscored names → behavioral name
- Method names embedded in test names (`testGetUser`, `handleSubmit`) → behavioral name
- `test` prefix, a JUnit 3 relic → remove
- Vague names (`testHappyPath`, `should work`) → specific behavior
- Deeply nested groups (>3 levels) → flatten or split the file

Group names are noun phrases or "when" clauses: `CommandsGenerateEvents`, `when user is
authenticated`. The language reference gives the exact naming pattern and grouping mechanism.

**Group-name de-duplication:** when a test moves into a group, trim the words the group name already
encodes. Group + test name should read as one phrase without repetition.

```java
// Before
@Test void endDateBeforeStartDateBlocksSubmit() { ... }
@Test void endTimeBeforeStartTimeOnSameDayBlocksSubmit() { ... }
// After
@Nested class ValidationBlocksSubmit {
    @Test void endDateBeforeStartDate() { ... }
    @Test void endTimeBeforeStartTimeOnSameDay() { ... }
}
```

**Output format:** a table of Current Name | Proposed Name | Proposed Group.

### Group D: Language-specific proposals

Apply the remaining proposal groups from the language reference — TypeScript: React Testing Library
anti-patterns and Playwright improvements; Java: modern-Java opportunities.

### Efficiency Heuristic

Before proposing any multi-file rename or repetitive transformation, check whether the IDE can do it
faster. If so, give the exact steps instead of editing each site:

> "This rename affects 47 usages across 12 files. Instead of editing each one, use IntelliJ: place
> the cursor on the symbol → Shift+F6 → type the new name → Enter." (VS Code: F2.)

## Phase 6: Execute Approved Refactorings

**Step 7:** Apply only what the user approved. Re-run the suite after each batch (per file or per
refactoring type). If a test breaks, revert the last change and investigate.

---

To improve this skill after a session, run `retro` — it owns skill edits, and it prunes as well as
adds.
