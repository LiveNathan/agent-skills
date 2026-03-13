# Paranoic Telemetry: Missing Test Audit

A workflow for auditing and completing test coverage on service/HTTP clients, based on [James Shore's Paranoic Telemetry pattern](https://www.jamesshore.com/v2/projects/nullables/testing-without-mocks#paranoic-telemetry).

---

## Workflow

1. **Analyze** the existing test file or context provided. Map what is already covered against the checklist below.
2. **List only the gaps** - not the full checklist, just what is missing or insufficient. For each missing test, give a one-line explanation of why it matters.
3. **Also flag** any existing tests that appear redundant, duplicated, or testing implementation details rather than behavior - these are candidates for removal.
4. **Ask**: "Want me to write these (and remove the flagged ones)?" Do not write any code until confirmed.

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

- [ ] **Makes request** - the outgoing call has the correct method, path, headers, and body
- [ ] **Parses response** - the correct value is extracted and returned from a valid response
- [ ] **Tracks requests** *(if using Shore's Nullable Infrastructure Wrapper)* - trackability works

If the endpoint handles collections, consider Zero-One-Many for happy path inputs:
zero items, one item, many items. Only add these if they would catch a real bug.

### Failure Paths ← the Paranoic Telemetry core

Each of the following must either throw an exception or log an error and send an alert.
Each thrown exception must include a **rich error message** (see below).

- [ ] **Unexpected status code** - non-2xx or any unexpected code throws
- [ ] **Missing body** - empty response body throws
- [ ] **Unparseable body** - malformed JSON or wrong content type throws
- [ ] **Wrong body shape** - JSON valid but expected field missing or wrong type throws
- [ ] **Extra fields in body** - body has MORE fields than expected → must NOT throw
      (this tests forward compatibility with API evolution)
- [ ] **Request timeout / hanging connection** - a connection that never responds throws

### Nullability *(only if using Shore's Nullable Infrastructure Wrapper pattern)*

- [ ] **Default null response** - `createNull()` returns a sensible default without
      making real network calls
- [ ] **Configurable null response** - `createNull({ response: "..." })` works
- [ ] **Forced error** - `createNull({ error: "..." })` throws the configured error

---

## Rich Error Messages

Every failure path test should assert on the *content* of the error message, not just
that an error was thrown. The message must include enough context to diagnose the
problem in production without needing to reproduce it.

Required fields in every failure message:

```
{Human-readable description of what went wrong}
Host: {host}:{port}
Endpoint: {path}
Status: {status code}
Headers: {headers as JSON}
Body: {raw response body}
```

Example (from Shore's ROT-13 client):

```
Unexpected status from ROT-13 service
Host: localhost:9999
Endpoint: /rot13/transform
Status: 400
Headers: {"content-type":"application/json"}
Body: {"error":"bad request"}
```

This is the telemetry part. When something breaks in production, this message is
what lets you diagnose it in seconds rather than hours.

---

## Test Structure

**Jest / TypeScript / JavaScript:**

```typescript
describe("ServiceName client", () => {
  describe("happy path", () => { ... })
  describe("failure paths", () => { ... })
  describe("nullability", () => { ... }) // omit if not using Nullable Infrastructure Wrapper
})
```

**JUnit 5 / Java:**

```java
class ServiceNameClientTest {
  @Nested class HappyPath { ... }
  @Nested class FailurePaths { ... }
  @Nested class Nullability { ... } // omit if not using Nullable Infrastructure Wrapper
}
```

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
happens in production, your code fails *loudly and informatively* rather than
silently or cryptically.

**Reference implementation:** Shore's ROT-13 client from the
["Microservice Clients Without Mocks, Part 2" livestream](https://www.jamesshore.com/v2/projects/nullables/testing-without-mocks),
tag `2020-09-01` on [github.com/jamesshore/livestream](https://github.com/jamesshore/livestream).
