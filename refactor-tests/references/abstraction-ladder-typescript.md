# Test Abstraction Ladder (TypeScript)

Progressively higher levels of abstraction for test setup and assertions, adapted for TypeScript. The goal is to maximize readability and maintainability while minimizing duplication.

## Level 1: Inline Everything

The simplest approach. All setup, execution, and assertion in the test body. Best for tests with minimal setup.

```typescript
test('new order has pending status', () => {
  const order = createOrder({ items: [] });

  expect(order.status).toBe('pending');
});
```

## Level 2: Named Variables for Clarity

When object literal values are ambiguous, extract to named constants to make their purpose evident.

```typescript
test('order total includes tax', () => {
  const preTaxAmount = 100;
  const taxRate = 0.08;

  const order = createOrder({ items: [{ price: preTaxAmount }], taxRate });

  expect(order.total).toBe(preTaxAmount * (1 + taxRate));
});
```

## Level 3: Mock Factory Functions (`Partial<T>` + Spread)

The TypeScript workhorse pattern for test data. Uses `Partial<T>` and the spread operator so callers specify only what matters for their specific test.

```typescript
// Define once, at the top of the test file or in a shared module
const defaultUser: User = {
  id: 'user-1',
  name: 'Default User',
  email: 'default@example.com',
  role: 'viewer',
  createdAt: new Date('2025-01-01'),
};

function createMockUser(overrides: Partial<User> = {}): User {
  return { ...defaultUser, ...overrides };
}

// Usage - only specify what matters for THIS test
test('admin users can delete projects', () => {
  const admin = createMockUser({ role: 'admin' });

  expect(canDeleteProject(admin)).toBe(true);
});
```

When to extract: when **2-3 tests** create similar objects with small variations.

## Level 4: Shared Factory Module

When factories are needed across **3+ test files**, move them to a shared module.

```typescript
// src/test-utils/factories/user.factory.ts
import type { User } from '@/types/user';

const defaults: User = {
  id: 'user-1',
  name: 'Default User',
  email: 'default@example.com',
  role: 'viewer',
  createdAt: new Date('2025-01-01'),
};

export function createMockUser(overrides: Partial<User> = {}): User {
  return { ...defaults, ...overrides };
}
```

## Level 5: Composed Factories for Nested Types

When objects contain other objects, compose factory calls. This keeps tests focused on the relevant data at each level.

```typescript
import { createMockUser } from './user.factory';

const defaults: Project = {
  id: 'proj-1',
  name: 'Default Project',
  owner: createMockUser(),
  members: [],
  createdAt: new Date('2025-01-01'),
};

export function createMockProject(overrides: Partial<Project> = {}): Project {
  return { ...defaults, ...overrides };
}

// Usage - compose factories for nested types
const project = createMockProject({
  owner: createMockUser({ role: 'admin' }),
});
```

**Important:** Use `structuredClone()` for deeply nested defaults to prevent mutation across tests.

## Level 6: Render Helpers (Component Test Specific)

For React components that require provider wrapping (theme, router, auth context).

```typescript
// src/test-utils/render.tsx
import { render, type RenderOptions } from '@testing-library/react';
import { ChakraProvider } from '@chakra-ui/react';
import { theme } from '@/theme';

function renderWithProviders(
  ui: React.ReactElement,
  options: RenderOptions = {},
) {
  function Wrapper({ children }: { children: React.ReactNode }) {
    return <ChakraProvider theme={theme}>{children}</ChakraProvider>;
  }

  return render(ui, { wrapper: Wrapper, ...options });
}

export { renderWithProviders as render };
```

When to extract: when **3+ component tests** share the same provider wrapping setup.

## Level 7: Custom Jest Matchers

For domain-specific assertions that provide better failure messages. See `references/custom-matchers.md`.

When to extract: when the same assertion pattern appears in **4+ tests**.

## Progression Guidance

Start at Level 1. Only climb when duplication or readability demands it:
- **Level 3** (mock factory): 2-3 tests create similar objects.
- **Level 4** (shared module): Factory imported in 3+ files.
- **Level 6** (render helper): 3+ component tests share provider setup.
- **Level 7** (custom matcher): Assertion pattern appears 4+ times.
