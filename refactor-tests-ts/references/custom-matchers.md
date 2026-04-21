# Custom Jest Matchers

Create domain-specific matchers for cleaner, more expressive test code. This is the TypeScript equivalent of custom AssertJ assertions in the Java `refactor-tests` skill.

## When to Create Custom Matchers

Create a custom matcher when you find tests repeatedly checking the same combination of properties on a domain object (4+ times), or when Jest's default matchers produce unhelpful failure messages.

## Template: Basic Custom Matcher

```typescript
// src/test-utils/matchers.ts (or jest.setup.ts)
expect.extend({
  toBeApprovedOrder(received: unknown) {
    const order = received as Order;
    const pass = order.status === 'approved' && order.approvedAt !== null;

    return {
      pass,
      message: pass
        ? () => `expected order NOT to be approved, but it was`
        : () => `expected order to be approved, but status was "${order.status}"`,
    };
  },
});
```

## TypeScript Type Declaration

Create a declaration file (`jest.d.ts`) so TypeScript knows about your custom matchers:

```typescript
declare global {
  namespace jest {
    interface Matchers<R> {
      toBeApprovedOrder(): R;
    }
  }
}
export {};
```

## Usage

```typescript
test('order is approved after payment', () => {
  const order = processPayment(createMockOrder({ status: 'pending' }));
  expect(order).toBeApprovedOrder();
});
```

## Template: Asymmetric Matcher

For use inside `expect.objectContaining` or `toEqual`:

```typescript
expect.extend({
  isoDateString(received: unknown) {
    const pass = typeof received === 'string' && !isNaN(Date.parse(received));
    return {
      pass,
      message: pass
        ? () => `expected "${received}" not to be a valid ISO date string`
        : () => `expected "${received}" to be a valid ISO date string`,
    };
  },
});

// Usage
expect(response.body).toEqual({
  createdAt: expect.isoDateString(),
});
```

## Registration and Setup

Register matchers in `jest.setup.ts`:

```typescript
// jest.setup.ts
import '@testing-library/jest-dom';
import './src/test-utils/matchers';
```

Reference in `jest.config.ts`:

```typescript
export default {
  setupFilesAfterSetup: ['<rootDir>/jest.setup.ts'],
};
```

## Naming Conventions

- **toBe**: State checks (`toBeApproved`, `toBePending`).
- **toHave**: Property checks (`toHaveValidationError`, `toHavePermission`).
- **toContain**: Collection checks (`toContainActiveUser`).

## Writing Good Failure Messages

The entire point of custom matchers is better failure messages. Use `this.utils.printReceived()` and `this.utils.printExpected()` for consistent formatting.

```typescript
message: () =>
  `expected ${this.utils.printReceived(received)} ` +
  `to be an approved order, but status was ` +
  `${this.utils.printReceived(order.status)}`
```
