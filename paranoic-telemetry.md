# Paranoic Telemetry: Missing Test Audit

A workflow for auditing and completing test coverage on service/HTTP clients, based on [James Shore's Paranoic Telemetry pattern](https://www.jamesshore.com/v2/projects/nullables/testing-without-mocks#paranoic-telemetry).

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
- Access modifiers are negotiable. If changing `private` to package-private (Java) or
  extracting a function makes a critical test cleaner, recommend it and include those
  tests in the list.
- Pruning is part of the job. Flag existing tests for removal when appropriate.

---

## The Checklist

### Happy Path

- [ ] Returns expected output - given valid input, the method returns the expected result
- [ ] Handles the primary use case - the core behavior works end to end
- [ ] Tracks calls _(if using Shore's Nullable Infrastructure Wrapper)_ - observability works

If the method handles collections, consider Zero-One-Many for inputs: zero items, one item, many items. Only add these if they would catch a real bug.

_Example (HTTP client): the outgoing request has the correct method, path, headers, and body; the response is correctly parsed and returned._

### Failure Paths ← the Paranoic Telemetry core

Each of the following must either throw an exception or log an error and send an alert.
Each thrown exception must include a rich error message (see below).

- [ ] Unexpected status code - non-2xx or any unexpected code throws
- [ ] Missing body - empty response body throws
- [ ] Unparseable body - malformed JSON or wrong content type throws
- [ ] Wrong body shape - JSON valid but expected field missing or wrong type throws
- [ ] Extra fields in body - body has MORE fields than expected → must NOT throw
      (this tests forward compatibility with API evolution)
- [ ] Request timeout / hanging connection - a connection that never responds throws

### Nullability _(only if using Shore's Nullable Infrastructure Wrapper pattern)_

- [ ] Parameterless instantiation - `createNull()` works without any arguments, even if the production `create()` requires them
- [ ] Isolation - a Nulled instance does not talk to the network or any external system
- [ ] Default response - calling a read method on a Nulled instance returns a sensible default rather than failing or returning null
- [ ] Configurable responses - `createNull()` can be configured to return specific values; if multiple calls are expected, it can return a sequence of responses or responses keyed by endpoint
- [ ] Output tracking - if the wrapper writes data, a `trackXxx()` method captures what would have been sent so you can assert on it
- [ ] Behavior simulation - if the wrapper responds to external events, a `simulateXxx()` method can trigger those events in application code
- [ ] Behavioral parity - the Embedded Stub mimics real-world behavior including asynchronous timing (e.g., `setImmediate`) so tests don't pass by coincidence due to synchronous execution
- [ ] Forced error - `createNull({ error: "..." })` throws the configured error

---

### Rich Error Messages

Every failure path test should assert on the _content_ of the error message, not just that an error was thrown. The message must include enough context to diagnose the problem in production without needing to reproduce it.

At minimum, include: what went wrong, what was being called, what was sent, and what came back. The exact fields depend on the type of external system - an HTTP client will log status, headers, and body; a file system wrapper will log the path and OS error; a queue consumer will log the message payload. The principle is the same: when this breaks at 2am, the log entry alone should tell you everything you need.

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

> "Assume they really are out to get you, and instrument your code accordingly.
> Expect that everything will break eventually. Test that every failure case either
> logs an error and sends an alert, or throws an exception that ultimately logs an
> error and sends an alert. Remember to test your code's ability to handle requests
> that hang, too."

The core idea: external services will fail - not might, will. File systems lose data.
Services return error codes, change their specs, and refuse to terminate connections.
Paranoic Telemetry means you test every one of those failure modes so that when it
happens in production, your code fails _loudly and informatively_ rather than
silently or cryptically.

Reference implementation: Shore's ROT-13 client from the
["Microservice Clients Without Mocks, Part 2" livestream](https://www.jamesshore.com/v2/projects/nullables/testing-without-mocks),
tag `2020-09-01` on [github.com/jamesshore/livestream](https://github.com/jamesshore/livestream).

---

## Known Edge Cases

*This section is maintained by the skill itself. Each entry is a generalized lesson
from a real session, added only when the workflow produced something awkward or missed
something real.*

<!-- Add entries here as: - [context] lesson learned -->
