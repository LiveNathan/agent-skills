---
name: paranoic-telemetry
description: Audit and complete test coverage for service/HTTP clients and infrastructure wrappers using James Shore's Paranoic Telemetry pattern. Use when writing or reviewing tests for external service clients, HTTP clients, domain services, event handlers, or file processors - especially to verify failure paths, rich error messages, Nullable Infrastructure Wrapper defaults, and forward-compatibility with API evolution. Triggers on requests to audit test coverage, find missing tests, or harden failure-mode testing.
version: "2.0"
author: nathanlively
compatibility: Language-agnostic; examples use Jest/TypeScript and JUnit 5/Java.
allowed-tools: Read, Grep, Bash(npm:*), Bash(./mvnw:*), Bash(jest:*)
---

## Workflow

1. Analyze the existing test file or context provided. Map what is already covered against the checklist below.
2. List only the gaps - not the full checklist, just what is missing or insufficient. For each missing test, give a one-line explanation of why it matters.
3. Also flag any existing tests that appear redundant, duplicated, or testing implementation details rather than behavior - these are candidates for removal.
4. Ask: "Want me to write these (and remove the flagged ones)?" Do not write any code until confirmed.

If no test file is provided, list all recommended tests for a client targeting the described service/endpoint, grouped by category.

---

## Efficiency Mindset

The goal is the minimum effective test suite - enough to catch real failures, easy to change.

- No redundant tests. If two tests verify the same behavior, one should go.
- No speculative tests. Only test what the code currently does.
- Access modifiers are negotiable. If changing `private` to package-private (Java) or extracting a function makes a critical test cleaner, recommend it and include those tests in the list.
- Pruning is part of the job. Flag existing tests for removal when appropriate.

---

## The Checklist

### Happy Path

- [ ] Returns expected output - given valid input, the method returns the expected result

- [ ] Handles the primary use case - the core behavior works end to end

- [ ] Tracks calls *(if using Shore's Nullable Infrastructure Wrapper)* - observability works

If the method handles collections, consider Zero-One-Many for inputs: zero items, one item, many items. Only add these if they would catch a real bug.

*Example (HTTP client): the outgoing request has the correct method, path, headers, and body; the response is correctly parsed and returned.*

### Failure Paths &lt;- the Paranoic Telemetry core

Each of the following must either throw an exception or log an error and send an alert. Each thrown exception must include a rich error message (see below).

**For HTTP clients:**

- [ ] Unexpected status code - non-2xx or any unexpected code throws

- [ ] Missing body - empty response body throws

- [ ] Unparseable body - malformed JSON or wrong content type throws

- [ ] Wrong body shape - JSON valid but expected field missing or wrong type throws

- [ ] Extra fields in body - body has MORE fields than expected -&gt; must NOT throw (this tests forward compatibility with API evolution)

- [ ] Request timeout / hanging connection - a connection that never responds throws

**For query builders / string-assembling components (SQL builders, template renderers, command constructors):**

- [ ] Injection safety - hostile input (SQL/HTML/shell metacharacters, e.g. `'; DROP TABLE x; --`) must land in bound parameters or escaped output, never in the assembled string. Assert the raw payload is ABSENT from the built string and PRESENT in `replacements`/params.

**For pure parsers / mappers (Zod/schema boundary, deserializers, row mappers — no logger, no external IO):**

The failure telemetry *is* the richness of the thrown error, since there is no logger and no IO to observe. A raw `ZodError` or `SyntaxError` (e.g. from `JSON.parse` inside a schema preprocess) leaks with no record identity or calling context.

- [ ] Unparseable input - malformed JSON / wrong type throws (not silently coerced)
- [ ] Wrong shape - missing required field, wrong type, invalid enum, invalid nested element throws
- [ ] Extra fields - unknown keys are ignored, must NOT throw (forward compatibility)
- [ ] Rich wrapped error - the parse is wrapped in try/catch that rethrows a domain error carrying the record identity (e.g. row id) + a context string + the original error as `cause`. Assert `isInstanceOf(YourMappingError)`, `hasMessageContaining(id)`, `hasMessageContaining("<context string>")`, and that `cause` is the original `ZodError`/`SyntaxError`.

**For non-HTTP components (domain services, event handlers, file processors, etc.):**

- [ ] Collaborator exceptions are caught, wrapped with rich context, and re-thrown - the wrapper exception message must include enough to diagnose the problem in production without reproducing it (e.g. identifiers, filename, calling context such as "on CsvDataCleaned" or "during startup sweep"). Assert `isInstanceOf(YourDomainException.class)`, `hasMessageContaining(id.toString())`, `hasMessageContaining("<context string>")`, and `hasCause(originalException)`.

### Nullability *(only if using Shore's Nullable Infrastructure Wrapper pattern)*

The goal of `createNull` is to make tests **easy to set up but hard to use incorrectly**. Defaults must enable parameterless instantiation of the full dependency tree, but those defaults should be inconvenient enough that a test relying on them by accident will produce obviously wrong results. When a test needs a specific value, it overrides the default explicitly - this keeps the "arrange" section of each test focused on exactly what that test cares about.

- [ ] Parameterless instantiation - `createNull()` works without any arguments, even if the production `create()` requires them

- [ ] Isolation - a Nulled instance does not talk to the network or any external system

- [ ] Inconvenient default - calling a read method on a Nulled instance returns a value that is valid but deliberately unusual, so that accidental reliance on it produces visibly strange results rather than silent false successes (e.g., a rare timezone like Lord Howe Island instead of UTC, a `503` status with body `"null http client default response"` instead of `200 OK`, a dummy locale like Manx instead of en-US)

- [ ] Fail-fast for mandatory config - when no safe unusual default exists for a dependency, `createNull()` without the required option throws an exception that documents how to use the API (e.g., `"must specify options.timeZone; use 'local' for computer's time zone"`)

- [ ] Configurable responses - `createNull()` can be configured to return specific values; if multiple calls are expected, it can return a sequence of responses or responses keyed by endpoint

- [ ] Output tracking - if the wrapper writes data, a `trackXxx()` method captures what would have been sent so you can assert on it

- [ ] Behavior simulation - if the wrapper responds to external events, a `simulateXxx()` method can trigger those events in application code

- [ ] Behavioral parity - the Embedded Stub mimics real-world behavior including asynchronous timing (e.g., `setImmediate`) so tests don't pass by coincidence due to synchronous execution

- [ ] Forced error - `createNull({ error: "..." })` throws the configured error

---

### Rich Error Messages

Every failure path test should assert on the *content* of the error message, not just that an error was thrown. The message must include enough context to diagnose the problem in production without needing to reproduce it.

At minimum, include: what went wrong, what was being called, what was sent, and what came back. The exact fields depend on the type of external system - an HTTP client will log status, headers, and body; a file system wrapper will log the path and OS error; a queue consumer will log the message payload. The principle is the same: when this breaks at 2am, the log entry alone should tell you everything you need.

### Logger Output Testing

When testing log entries (particularly with nullables that use EventEmitter-based loggers), create the `OutputTracker` **before** calling the method under test, not after. The tracker only captures log events emitted after it subscribes to the logger's event emitter.

**Correct pattern:**

```typescript
const logger = controller.getLogger();
const outputTracker = logger.trackOutput(); // <-- BEFORE

await controller.handleRequest(req, res);

expect(outputTracker.data).toContainEqual({
  level: "warn",
  message: "Expected message",
  metadata: {key: "value"},
});
```

**Incorrect pattern:**

```typescript
await controller.handleRequest(req, res); // <-- Logs emitted here
const outputTracker = controller.getLogger().trackOutput().data; // <-- TOO LATE

expect(outputTracker).toContainEqual({...}); // Will always fail or be empty
```

This applies to any logger that uses event-based output tracking (e.g., EventEmitter with `trackOutput()` methods). If you retrieve the tracker after the method has run, it will be empty because it missed the events.

**When the logger is console-based (no `trackOutput`/EventEmitter):** you cannot assert log content without a `console.*` spy. Either (a) recommend making the logger a nullable infrastructure wrapper so failure-path telemetry is assertable, or (b) explicitly flag the telemetry as unverified and test only the return/throw. Do not silently skip — surface it.

### Boundary Data Must Actually Sit On The Boundary

When you add a test for a boundary or edge case, **pick data that provably fails against the naive
implementation.** Then verify it: temporarily revert the code to the naive version (or write the
test before the fix) and confirm the test goes red. If it passes either way, it is not a boundary
test — it is a test whose *name* claims a risk it never exercises.

This is worse than having no test at all, because the name retires the concern. A reviewer scanning
for "is the month-boundary case covered?" sees that it is, and stops looking.

Real example: a rule specified as `newEndDate = oldEndDate + (newStartDate - oldStartDate)` was
implemented with a calendar `Period`, which clamps to month length. A test named
`multiDaySpanShiftedAcrossAMonthBoundaryPreservesSpanLength` used Oct 30 → Nov 2 and passed under
both the broken and correct implementations. The bug — a 16-day activity silently becoming 13 days
— needed a *clamping* day-of-month (29–31 into a shorter month) to surface, and was caught in
review instead.

Ask, for each boundary test: **which specific wrong implementation does this datum rule out?** If
you cannot name one, change the datum. Common cases where the obvious value is too weak: dates
(month lengths, leap days, DST), off-by-one indices (pick 0 and n, not 1 and n-1), and numeric
limits (pick the value that overflows, not one near it).

---

## Test Structure

Jest / TypeScript / JavaScript:

```typescript
describe("ServiceName client", () => {
  describe("happy path", () => { ... })
  describe("failure paths", () => { ... })
  describe("nullability", () => { ... }) // omit if not using Nullable Infrastructure Wrapper
})
```

JUnit 5 / Java:

```java
class ServiceNameClientTest {
  @Nested class HappyPath { ... }
  @Nested class FailurePaths { ... }
  @Nested class Nullability { ... } // omit if not using Nullable Infrastructure Wrapper
}
```

---

Step 5 (after writing tests, if tests were written): Reflect briefly on the session. Did any test cases come up that the checklist doesn't cover? Did the workflow break down or produce something awkward? If so, propose a specific, minimal edit to this skill file - a new checklist item, a clarification, or a correction. Do not propose edits for things that already worked well. Present the proposal and ask whether to apply it.

---

## Background

From Shore:

> "Assume they really are out to get you, and instrument your code accordingly. Expect that everything will break eventually. Test that every failure case either logs an error and sends an alert, or throws an exception that ultimately logs an error and sends an alert. Remember to test your code's ability to handle requests that hang, too."

The core idea: external services will fail - not might, will. File systems lose data. Services return error codes, change their specs, and refuse to terminate connections. Paranoic Telemetry means you test every one of those failure modes so that when it happens in production, your code fails *loudly and informatively* rather than silently or cryptically.

Reference implementation: Shore's ROT-13 client from the ["Microservice Clients Without Mocks, Part 2" livestream](https://www.jamesshore.com/v2/projects/nullables/testing-without-mocks), tag `2020-09-01` on [github.com/jamesshore/livestream](https://github.com/jamesshore/livestream).

---

## Division of Labor with Refactor-Tests

This skill audits **coverage correctness**: are the right tests present, are failure modes covered, are `createNull` defaults properly inconvenient? It writes new tests and flags redundant ones for removal.

It does NOT handle test readability, naming conventions, assertion descriptions, structural refactoring (builders, factory methods, custom assertions), or organization (`@Nested` groupings). Those concerns belong to the `refactor-tests` skill, which is designed to run after this one.

Workflow: run `paranoic-telemetry` first to get coverage right, then run `refactor-tests` to clean up how the tests read.

---

## Known Edge Cases

*This section is maintained by the skill itself. Each entry is a generalized lesson from a real session, added only when the workflow produced something awkward or missed something real.*

&lt;!-- Add entries here as: - \[context\] lesson learned --&gt;

- [date-shift rule, showbook 2026-07-25] A boundary test can name the right risk and still prove
  nothing if its data passes under the broken implementation too. Verify boundary tests go red
  against the naive version — see "Boundary Data Must Actually Sit On The Boundary".
- [pure projector/decider slices, showbook 2026-07-25] When a slice has no external infrastructure
  boundary (no HTTP, clock, LLM, or persistence wrapper), most of this checklist does not apply and
  forcing it produces ceremony. The findings that *do* generalize are the representable-input
  matrix: take the value object's fields and cross them null/non-null. Two real bugs came from
  exactly that — `endDate` present with a null `endTime` threw an NPE that a swallowing rebuild
  loop turned into a blank read model, and an equal start/end instant produced a zero-length bar.
  Neither was a named scenario; both were legal inputs nobody had crossed.

- \[UI/form views\] When validation errors may surface in multiple places (field-level invalid state, binder status label, notification), assert on the *behavioral outcome* (submit blocked, nothing persisted) rather than the specific error-display location. Field-level constraints (e.g. DatePicker min/max set by value-change listeners) can mark a field invalid before the bean-level validator runs, so the status label may stay empty even when validation correctly blocks submit.

- \[collaborators with multiple internal code paths\] Before writing a failure/branch test, check whether *every* branch of the method actually routes through the nulled dependency — some branches may call a real, un-nulled collaborator (e.g. a different internal client) even though the class's `createNull()` looks complete. Pick the branch/config that stays inside the null boundary rather than reaching for `jest.mock`.
