# TypeScript: tooling and idioms

Targets TypeScript with Jest, React Testing Library, and Playwright. Companion references:
`abstraction-ladder-typescript.md`, `custom-matchers.md`, `eslint-automated-cleanup.md`.

## Phase 1: detection

**Test category mix in scope:**

- Pure logic tests (Jest only — utility functions, validators, data transforms)
- Component tests (Jest + React Testing Library)
- Hook tests (Jest + `renderHook`)
- API route tests (Jest + `node-mocks-http`)
- E2E tests (Playwright)

**Exclusions.** Identify them by file naming convention — `*.test.ts`/`*.test.tsx` (unit),
`*.integration.test.ts` (integration), Playwright tests in their own directory — then present:

> "I found these test categories: X unit tests, Y integration tests, Z Playwright tests. I'll run
> unit tests first to confirm green. Want to include integration tests? (They may require external
> services.)"

**Environment.** Detect the package manager (`pnpm-lock.yaml`, `yarn.lock`, `package-lock.json`) and
the runner config (`jest.config.ts`/`js`, `playwright.config.ts`).

## Phase 2: baseline command

```bash
pnpm test --silent          # or: pnpm run test:unit --silent
```

## Phase 3: ESLint auto-fix

Read `eslint-automated-cleanup.md` for the plugin list. Check what is installed:

```bash
pnpm list eslint-plugin-jest eslint-plugin-testing-library eslint-plugin-jest-dom
```

If installed, run `pnpm eslint --fix 'src/**/*.test.{ts,tsx}'`. If not, present the applicable
plugins and offer to install (`pnpm add -D ...`), add the config to `.eslintrc`/`eslint.config.js`,
then run the fix. Re-run `pnpm test --silent`.

## Phase 4: automatic refactorings

- **Use `screen` for all RTL queries** — replace queries destructured from `render()`.
- **Replace `fireEvent` with `userEvent`** for user interactions (typing, clicking, selecting). This
  makes tests async: ensure the test function is `async` and awaits.
- **Correct query variants** — `getBy*` for elements that should exist (throws on missing, the good
  default); `queryBy*` **only** for asserting non-existence; `findBy*` for elements that appear
  asynchronously.
- **Prefer accessible queries** — priority order: `getByRole`, `getByLabelText`,
  `getByPlaceholderText`, `getByText`, `getByDisplayValue`, `getByAltText`, `getByTitle`,
  `getByTestId` (last resort).
- **Use jest-dom matchers** — `.toBeDisabled()`, `.toHaveTextContent()`, `.toBeInTheDocument()`
  instead of manual DOM property checks.
- **Remove unnecessary `act()`** — `render`, `fireEvent`, and `userEvent` already wrap in `act()`.
- **Fix `waitFor` misuse** — no side-effects in the callback, one assertion per callback, and
  `findBy*` instead of `waitFor` + `getBy*`.

## Phase 5: readability and structure

Group A additions: **`act()` warnings in the console** — fix the root cause rather than silencing.

Group B rungs are TypeScript-shaped — mock factory functions using `Partial<T>` + spread, shared
factory modules under `src/test-utils/factories/` once used across 3+ files, composed factories for
nested types, custom Jest matchers, and `renderWithProviders` helpers for components with heavy
context requirements.

## Phase 5: naming and organization

- `describe` blocks name the unit under test; `test`/`it` name the case — be consistent within a file
  (`eslint-plugin-jest`'s `consistent-test-it` enforces this).
- Pattern: `'<expected outcome> when <condition>'` or `'<action> <expected result>'`. Lowercase
  starts (`jest/prefer-lowercase-title`).
- Good: `'returns empty array when no items match the search term'`, `'disables submit button while
  form is submitting'`, `'throws validation error for negative quantities'`.
- Nested `describe` names are noun phrases or "when" clauses.

## Group D: React Testing Library anti-patterns

1. `container.querySelector()` instead of Testing Library queries
2. Not using `screen`
3. `queryBy*` for existence checks
4. Unnecessary `act()` wrapping
5. `fireEvent` where `userEvent` fits better
6. Side-effects inside `waitFor`
7. `waitFor` + `getBy*` instead of `findBy*`
8. Unnecessary `role` attributes in components

## Group D: Playwright improvements (if E2E is in scope)

1. Replace CSS selectors with locators (`page.getByRole`)
2. Use web-first assertions (`await expect(locator).toBeVisible()`)
3. Propose a Page Object Model once 3+ tests touch the same page
4. Group with `test.describe`
5. Tag tests (e.g. `{ tag: '@smoke' }`)

## TypeScript-specific considerations

- **Type-safe mock factories** — use `Partial<T>`; avoid `as unknown as T`.
- **Discriminated unions** — custom matchers should leverage type narrowing.
- **Zod schemas** — test `.parse()` / `.safeParse()` behavior.
- **Async/await** — ensure every async operation and assertion is awaited.
- **Type imports** — use `import type`.
- **Infrastructure Wrappers / Nullables** — preserve Shore's patterns; do not replace with
  `jest.mock()`.
