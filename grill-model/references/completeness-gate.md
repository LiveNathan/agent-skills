# The Information Completeness Gate

**The gate is per-SLICE, not per-chapter.** Run it once per slice and report a result per slice.
A chapter with three passing slices and one carrying an open hotspot is not a failed chapter —
it is three slices ready to build and one still being designed. Slices are independent building
blocks and can be implemented in any order, so blocking all of them on the weakest one is
self-inflicted.

What is *not* negotiable is the individual slice: a slice with a red arrow — any attribute
without a traceable data trail — cannot be coded, because you would be building on an unverified
assumption. That is the mistake that produces "tests based on data that isn't actually available
in the source events." One well-executed slice is worth more than four half-finished ones.

Run it as a literal checklist and report each line's result — "looks fine" is not a result.

The underlying rule, in one sentence: **every piece of information displayed or written by the
system must be traceable, backwards through the model, to the point where a human or an
external system first supplied it.** On prooph board this is the "all arrows black" property.
A red arrow means data appears from nowhere, and it will stall implementation the moment
someone tries to write the code.

---

## 1. Field-level traceability

For **every field** of **every read model / information element** in the chapter:

- [ ] It is derived from at least one event in the model.
- [ ] That event's own version of the field is traceable to a command payload, to prior state
      the command legitimately read, or to an external system via a translation/automation.
- [ ] The chain terminates at a UI form field, an API payload, or an external source — not at
      "the system knows this".

For **every field** of **every event**:

- [ ] It comes from the command that emitted it, from state the decider had, or from a
      documented external source.
- [ ] It is a **business fact**, not a technical artifact (no `updatedAt` smuggling in as the
      only reason the field exists).

Walk this backwards, field by field, not forwards. Forwards feels complete; backwards finds
the holes. The classic miss: a comparison rule ("most recent wins") whose *second* input never
actually reaches the decision point.

**Where to look:** field-level payload definitions belong on the elements themselves — run the
`schema` skill for the formal shape and `example-data` for the concrete YAML values. This gate
checks that the *chain between them* is unbroken; it does not restate how to write either.

## 2. Command completeness

For every command:

- [ ] Every field it needs is supplied by its triggering UI or automation — nothing is assumed
      to be ambiently available.
- [ ] It has a stated trigger (a preceding Read slice or Automation slice).
- [ ] It emits at least one event on success.
- [ ] Its **rejection conditions** are enumerated, and each one's outcome is modeled — an error
      shown to the user, a rejection event, or an explicit "nothing happens".
- [ ] Where a rejection must be *remembered* (audit, retry, dead-letter), a rejection event
      exists rather than a thrown exception that vanishes.

## 3. Read model completeness

For every read model:

- [ ] Every event that should update it is wired to it — including deletions, corrections, and
      status transitions, which are the ones routinely forgotten.
- [ ] Its initial/empty state is defined (before any event has occurred).
- [ ] Every field the UI displays exists on it. Cross-check against the UI element, not against
      intuition.
- [ ] If it's rebuilt by replay, nothing in it depends on wall-clock time at projection time.

## 4. Automation completeness

For every automation:

- [ ] Its trigger is a specific event (or a schedule, explicitly stated).
- [ ] What it reads is modeled as an actual information element, not implied.
- [ ] Its rule is stated concretely enough to be a test — including the tie-breaker when two
      inputs are equal.
- [ ] Its issued command is in the model.
- [ ] Its **termination policy** is stated: what on failure, how many retries, and what happens
      after the last one.
- [ ] It cannot trigger itself, directly or through a cycle.

## 5. Scenario sufficiency

Run the `slice-scenarios` skill's own "Checklist for Writing Slice Scenarios" against every
slice. Do not duplicate it here — load it and work it. Then add the one check that skill can't
make, because it doesn't know what happens downstream:

- [ ] **Could a test author with no access to the production code write failing tests from these
      scenarios alone?** That is literally the next step in this pipeline: `test-writer` runs
      before any implementation exists and has nothing but these scenarios. If the answer is no,
      the gate fails here — not in Build, where it costs a round-trip and an escalation.

## 6. Narrative completeness

- [ ] Read the chapter left to right as a story. It should be a coherent sequence a business
      person would recognize, with no unexplained jumps.
- [ ] Every slice is one type. No mixed element sets.
- [ ] The chapter starts where its actor does — a **UI** slice for a journey a human initiates, a
      Read or Automation slice for one the system starts on its own. A chapter a person walks
      through with no UI element anywhere cannot say where they entered or where they ended up,
      and that is a gap a reader notices immediately even when every command is well specified.
- [ ] Alternative *journeys* are separate chapters; conditional *outcomes of one trigger* are
      sibling slices here.

## 7. Agreement

- [ ] Every open question is a Hotspot on the board, not an assumption in someone's head.
- [ ] The business rules are ones the user has actually confirmed, not ones you proposed and
      they didn't object to. Silence is not agreement — if a rule was never explicitly
      confirmed, it's still a Hotspot.
- [ ] Any board-vs-code contradiction found while grilling is resolved on the board.

---

## Reporting the result

One block per slice, then a chapter roll-up. Checks 6 (narrative) and 7 (agreement) are
chapter-level; the rest are per-slice.

```
COMPLETENESS GATE — <Chapter>

Slice 1 "<name>":  PASS | FAIL
  1. Field traceability   PASS | FAIL — <what, if failed>
  2. Commands             PASS | FAIL — …
  3. Read models          PASS | FAIL — …
  4. Automations          PASS | FAIL — …
  5. Scenarios            PASS | FAIL — …
Slice 2 "<name>":  …

Chapter-level:
  6. Narrative            PASS | FAIL — …
  7. Agreement            PASS | FAIL — …

DESIGN COMPLETE: <slices that passed the gate, in dependency order>
BUILDABLE NOW:   <of those, the ones whose infrastructure actually exists today>
BLOCKED:         <every slice not buildable, each with what blocks it —
                  a hotspot (design) or a missing dependency (infrastructure)>
Hotspots opened: <list with board links>
```

**`DESIGN COMPLETE` and `BUILDABLE NOW` are different questions, and merging them is a real
mistake.** Everything above measures only the first: whether the model is specified well enough
to implement. It says nothing about whether the code it needs exists yet.

So before writing `BUILDABLE NOW`, check each passing slice for a **dependency** blocker:

- [ ] The events it folds are authored by a slice that has actually shipped — or it is honest
      about rendering an empty state until then.
- [ ] The infrastructure its commands need exists: the command processor, the event-log variant,
      the base types. A chapter writing to a new kind of stream is the classic case — the read
      path may have been split out long ago while the *write* orchestrator never got its sibling,
      because nothing had needed one before.
- [ ] Types it imports from a neighbouring chapter are merged to the main branch, not sitting on
      an unmerged branch.
- [ ] Any architecture-test allow-list it needs to appear on has been identified.

Report a slice as blocked-on-dependency in exactly the same breath as blocked-on-hotspot. Both
mean "do not start this," and only one of them is visible on the board.

A chapter whose design is complete but whose infrastructure is half-built is a normal, healthy
state — it just means the Build sessions have an order. Saying "all 8 buildable" when 6 of them
need a write path nobody has written is how a Build session discovers the problem at the most
expensive possible moment.

Any FAIL fails **that slice** — partial credit does not exist within a slice, because the cost
of implementing against an unverified assumption is paid by the Build session at a much worse
exchange rate. A failed slice blocks only itself and whatever depends on it.

One exception: if check 6 or 7 fails, the whole chapter is suspect — a broken narrative or an
unconfirmed business rule usually means the slice boundaries themselves are wrong, and building
any of it would be premature.
