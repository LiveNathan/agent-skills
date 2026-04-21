---
name: refactor-tests-ts
description: Refactor and modernize TypeScript test suites for readability, maintainability, and modern idioms. Use when the user asks to clean up tests, refactor tests, improve test readability, modernize test code, reduce test duplication, apply test best practices, or run test cleanup on a TypeScript/Next.js codebase. Targets TypeScript with Jest, React Testing Library, and Playwright. Do not use for writing new tests from scratch or for non-TypeScript projects. For Java projects, use the sibling skill refactor-tests instead.
---

# Refactor Tests (TypeScript)

Analyze and refactor TypeScript test code for readability, maintainability, and modern best practices. This skill adapts the core philosophy of the sibling Java `refactor-tests` skill for the TypeScript/Next.js ecosystem.

## Guiding Philosophy

- **Tests are living documentation**: They should tell other developers how to use the code and what behavior to expect.
- **Readability in isolation**: A developer should understand any single test without looking elsewhere.
- **Explicit over clever**: Prefer verbosity and clarity over complex abstractions or hidden setup.
- **Test behavior, not implementation**: "Test user-visible behavior, not implementation details" (Kent C. Dodds' Testing Library philosophy).
- **Respect existing architectural patterns**: (Infrastructure Wrappers, Nullables, OutputTracker) - do not replace them with conventional mocks unless the user requests it.
- **Query the DOM like a user**: Prefer accessibility-driven queries (getByRole, getByLabelText).

## Phase 1: Scope and Configuration

**Step 1:** Determine scope (single file or all test files). Also identify the test category mix in scope:
- Pure logic tests (Jest only - utility functions, validators, data transforms)
- Component tests (Jest + React Testing Library)
- Hook tests (Jest + `renderHook` from `@testing-library/react`)
- API route tests (Jest + `node-mocks-http`)
- E2E tests (Playwright)

**Step 2:** Identify test filtering. Scan for test file naming conventions:
- `*.test.ts` / `*.test.tsx` (unit tests)
- `*.integration.test.ts` (integration tests)
- Playwright tests in separate directory

Present the user with a summary:

> "I found these test categories: X unit tests, Y integration tests, Z Playwright tests. I'll run unit tests first to confirm green. Want to include integration tests? (They may require external services.)"

Wait for confirmation before proceeding.

**Step 3:** Detect the package manager (look for `pnpm-lock.yaml`, `yarn.lock`, or `package-lock.json`) and test runner configuration (`jest.config.ts`/`js`, `playwright.config.ts`).

## Phase 2: Go Green

**Step 4:** Run the test suite with quiet output:

```bash
pnpm test --silent
```

Or if using a custom script:

```bash
pnpm run test:unit --silent
```

**Step 5:** If any tests fail, stop and report failures to the user. Do not proceed with refactoring until all included tests pass. Refactoring from red is unsafe.

## Phase 3: ESLint Automated Cleanup

**Step 6:** Before any manual refactoring, check whether ESLint plugins can handle mechanical cleanups. Read `references/eslint-automated-cleanup.md` for the full plugin list and instructions.

Check which plugins are already installed:

```bash
pnpm list eslint-plugin-jest eslint-plugin-testing-library eslint-plugin-jest-dom
```

If plugins are installed, run auto-fix on test files:

```bash
pnpm eslint --fix 'src/**/*.test.{ts,tsx}'
```

If plugins are NOT installed, present the user with applicable plugins and offer to install them. Once approved:

```bash
pnpm add -D eslint-plugin-jest eslint-plugin-testing-library eslint-plugin-jest-dom
```

Then add configurations to `.eslintrc` / `eslint.config.js` and run the fix.

After ESLint fixes, re-run tests:

```bash
pnpm test --silent
```

Confirm green before proceeding to Phase 4.

## Phase 4: Automatic Refactorings

Apply these refactorings without asking for confirmation. They are safe and universally beneficial.

### 4a: Ensure AAA Structure with Blank Line Separation

Each test should have visible separation between Arrange, Act, and Assert sections. Add blank lines between sections if missing.

### 4b: Use `screen` for All React Testing Library Queries

Replace destructured queries from `render()` with `screen.*` queries.

### 4c: Replace `fireEvent` with `userEvent` Where Appropriate

For user interactions (typing, clicking, selecting), prefer `@testing-library/user-event`. This changes tests to async. Ensure the test function is `async` and uses `await`.

### 4d: Use Correct Query Variants

- `getBy*` for elements that should exist (throws on missing - good default).
- `queryBy*` ONLY for asserting non-existence (`expect(screen.queryByRole('alert')).not.toBeInTheDocument()`).
- `findBy*` for elements that appear asynchronously (`await screen.findByText('loaded')`).

### 4e: Prefer Accessible Queries (Query Priority)

Flag tests using low-priority queries when higher-priority alternatives exist. Priority order: `getByRole`, `getByLabelText`, `getByPlaceholderText`, `getByText`, `getByDisplayValue`, `getByAltText`, `getByTitle`, `getByTestId` (last resort).

### 4f: Use jest-dom Matchers

Replace manual DOM property checks with `@testing-library/jest-dom` matchers (e.g., `.toBeDisabled()`, `.toHaveTextContent()`, `.toBeInTheDocument()`).

### 4g: Remove Unnecessary `act()` Wrapping

`render()` and `fireEvent` (and `userEvent`) already wrap in `act()`. Remove redundant wrapping.

### 4h: Fix `waitFor` Misuse

- No side-effects inside `waitFor` callbacks.
- Only one assertion per `waitFor` callback.
- Use `findBy*` instead of `waitFor` + `getBy*`.

## Phase 5: Analysis and Proposals

Read each test file and identify refactoring opportunities. Present to the user in groups.

### Group A: Readability Quick Wins

1. **Unnamed magic values**: Extract to named constants.
2. **Irrelevant data not marked as dummy**: Use factory defaults for irrelevant parameters.
3. **Hidden setup in `beforeEach`**: Propose inlining setup that is tightly coupled to specific tests.
4. **Excessive `test.each` entries**: Remove redundant boundary condition checks.
5. **`act()` warnings in console**: Investigate and fix root cause rather than silencing.

### Group B: Structural Refactorings

Read `references/abstraction-ladder.md` for detailed guidance on when to apply each level.

1. **Extract mock factory functions**: Use `Partial<T>` + spread pattern for objects created with small variations.
2. **Create shared mock factory files**: Move factories to `src/test-utils/factories/` if used across 3+ files.
3. **Compose factories for nested types**: Call nested factories instead of deeply nested literals.
4. **Create custom Jest matchers**: For repeated complex domain assertions. Read `references/custom-matchers.md`.
5. **Extract render helpers**: Create `renderWithProviders` for components with complex context/provider requirements.

### Group C: Naming & Organization 

**Naming Convention:**

- Use `describe` blocks for the unit under test (function name, component name, hook name)
- Use `test` or `it` for individual cases (be consistent within a file - the `eslint-plugin-jest` rule `consistent-test-it` enforces this)
- Test names are plain English strings that read as behavioral statements
- Pattern: `'<expected outcome> when <condition/scenario>'` or `'<action> <expected result>'`
- Good: `'returns empty array when no items match the search term'`
- Good: `'disables submit button while form is submitting'`
- Good: `'throws validation error for negative quantities'`
- NEVER include function/method names in test names when it would make them fragile to renaming
- Prefer lowercase starts (enforced by `jest/prefer-lowercase-title` ESLint rule)

**Anti-patterns to flag:**

- `test('test getUser')` -> `test('returns user data for a valid ID')`
- `test('should work')` -> `test('calculates total with tax included')`
- `test('happy path')` -> specific behavioral description
- `test('handleSubmit')` -> `test('submits form data to the API')`
- Deeply nested `describe` (>3 levels) -> flatten or split file

**Organization:**

- Propose `describe` groupings where tests cluster by feature or scenario
- Nested `describe` names should be noun phrases or "when" clauses: `describe('when user is authenticated', ...)`

**Output format:** Present as a table:

|  Current Name |  Proposed Name |  Proposed `describe` Group |  
| ---- | ---- | ----  |
|   |   |   | 

### Group D: React Testing Library Anti-patterns

Flag these common mistakes:
1. Using `container.querySelector()` instead of Testing Library queries.
2. Not using `screen`.
3. Using `queryBy*` for existence checks.
4. Wrapping in unnecessary `act()`.
5. Using `fireEvent` when `userEvent` is more appropriate.
6. Side-effects inside `waitFor`.
7. `waitFor` + `getBy*` instead of `findBy*`.
8. Unnecessary `role` attributes in components.

### Group E: Playwright Improvements (if E2E tests are in scope)

1. Replace CSS selectors with Playwright locators (`page.getByRole`).
2. Use web-first assertions (`await expect(locator).toBeVisible()`).
3. Propose Page Object Model if 3+ tests interact with the same page.
4. Use `test.describe` for grouping.
5. Tag tests appropriately (e.g., `{ tag: '@smoke' }`).

## Efficiency Heuristic

Before proposing any multi-file rename or repetitive transformation, check whether VS Code's refactoring tools or ESLint auto-fix can do it faster. If so, provide the exact steps:

> "This rename affects 12 usages across 5 files. Use VS Code: place cursor on the symbol -> F2 -> type new name -> Enter."

## Phase 6: Execute Approved Refactorings

**Step 7:** Apply only approved changes. Re-run tests after each batch: `pnpm test --silent`.

## Phase 7: Reflection and Self-Improvement

**Step 8:** Write reflection entry to `journal/reflection-log.md`.
**Step 9:** Review reflection for skill improvement opportunities.

## TypeScript-Specific Considerations

- **Type-safe mock factories**: Use `Partial<T>`, avoid `as unknown as T`.
- **Discriminated unions**: Custom matchers should leverage type narrowing.
- **Zod schema testing**: Test `.parse()` / `.safeParse()` behavior.
- **Async/await**: Ensure proper `await` on all async operations and assertions.
- **Type imports**: Use `import type`.
- **Infrastructure Wrappers / Nullables**: Preserve James Shore's patterns; do not replace with `jest.mock()`.
