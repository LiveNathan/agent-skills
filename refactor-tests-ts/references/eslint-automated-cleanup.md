# ESLint Plugins for Automated Test Cleanup

These plugins provide reliable, automated cleanup before manual refactoring. Run with `--fix` to auto-apply fixable rules. This is the TypeScript equivalent of the OpenRewrite recipes used in the Java `refactor-tests` skill.

## Plugin 1: eslint-plugin-jest

Enforces Jest best practices.

Install: `pnpm add -D eslint-plugin-jest`

### Recommended Config Rules (auto-enabled with `plugin:jest/recommended`)

| Rule | Description | Fixable |
|---|---|---|
| `jest/valid-expect` | Correct `expect()` usage (no dangling) | Yes |
| `jest/no-alias-methods` | Use canonical names (`.toBeCalled` -> `.toHaveBeenCalled`) | Yes |
| `jest/no-identical-title` | No duplicate test names within a describe | No |
| `jest/no-jasmine-globals` | No Jasmine-style `fail()` or `pending()` | Yes |

### Style Config Rules (enable with `plugin:jest/style`)

| Rule | Description | Fixable |
|---|---|---|
| `jest/prefer-to-be` | `.toBe(null)` -> `.toBeNull()`, etc. | Yes |
| `jest/prefer-to-contain` | Verbose array check -> `.toContain(x)` | Yes |
| `jest/prefer-to-have-length` | `.length` check -> `.toHaveLength(n)` | Yes |

### Additional Recommended Rules

| Rule | Description | Fixable |
|---|---|---|
| `jest/consistent-test-it` | Enforce `test` vs `it` consistency | Yes |
| `jest/padding-around-all` | Enforce blank lines around test blocks | Yes |
| `jest/prefer-lowercase-title` | Lowercase test descriptions | Yes |

## Plugin 2: eslint-plugin-testing-library

Enforces React Testing Library best practices.

Install: `pnpm add -D eslint-plugin-testing-library`

| Rule | Description | Fixable |
|---|---|---|
| `testing-library/prefer-screen-queries` | Use `screen.*` over destructured queries | Yes |
| `testing-library/prefer-find-by` | Use `findBy*` over `waitFor` + `getBy*` | Yes |
| `testing-library/no-wait-for-side-effects` | No side-effects in `waitFor` | No |
| `testing-library/no-container` | Don't query via `container` | No |

## Plugin 3: eslint-plugin-jest-dom

Enforces @testing-library/jest-dom matcher usage over manual DOM checks.

Install: `pnpm add -D eslint-plugin-jest-dom`

| Rule | Description | Fixable |
|---|---|---|
| `jest-dom/prefer-to-have-text-content` | `.textContent` -> `.toHaveTextContent()` | Yes |
| `jest-dom/prefer-in-document` | Null check -> `.toBeInTheDocument()` | Yes |
| `jest-dom/prefer-enabled-disabled` | `.disabled` -> `.toBeDisabled()` | Yes |

## Running Automated Cleanup

Step 1: Install missing plugins.
Step 2: Run ESLint fix on test files:

```bash
pnpm eslint --fix 'src/**/*.test.{ts,tsx}'
```

Step 3: Re-run tests to confirm green: `pnpm test --silent`.
Step 4: Commit automated changes before manual refactoring.

## Notes

- Always run automated cleanup BEFORE manual refactoring.
- ESLint `--fix` only applies fixable rules. Non-fixable rules should be reviewed manually during Phase 5.
- If rules conflict, prefer the Testing Library plugin's opinions.
