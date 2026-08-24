---
name: grill-model
description: Harden an event model until it passes the information-completeness gate. The Validate phase of Storm-Design-Validate-Build - run it after sketching a chapter on prooph board and before any code is written. Ends by handing off to /prewalk.
argument-hint: "Chapter deeplink, chapter title, or issue URL"
---

You are running the **Validate** phase of the Storm → Design → Validate → Build cycle.

The goal is not "discuss the model." The goal is to drive one chapter to a state where it
**passes the completeness gate** in `references/completeness-gate.md` — meaning any developer
(or agent) could implement it without another conversation. You are done when the gate passes,
not when you run out of questions.

## Load project config first

Every project wires this skill differently. Before anything else, read the host project's
`CLAUDE.md` for an **Event modeling config** block, which tells you:

- the prooph board workspace id, and how chapters map to work items (gh issues, Asana tasks, …)
- where durable decisions go (`docs/adr/`, or nothing)
- where the design record goes (`docs/spec/…`, `docs/prd/…`)
- where the narrative log goes (`docs/journal/YYYY-MM.md`, or nothing)
- the project's build/test command, for the Build phase you're preparing

If there is no such block, ask for these once and offer to write the block into `CLAUDE.md`
so the next session doesn't ask again.

## Ground rules

- Load the `event-modeling` skill and enforce its rules — slice types, lane placement, valid
  flows, command eligibility, anti-patterns, and its Validation Checklist. You are the Critic.
- **Delegate to the specialist prooph board skills rather than duplicating their rules here:**
  `slice-scenarios` (GWT structure + checklist), `example-data` (concrete YAML on commands,
  events, information), `schema` (field-level payload schemas), `ascii-mockups` /
  `wireframe-sketch` (UI elements). If you catch yourself writing modeling rules into this file,
  the rule probably already lives in one of those — go use it instead.
- Run the interview per the `grilling` skill: **one question at a time**, each with your
  recommended answer stated first, waiting for the reply before the next.
- **Ask with the `AskUserQuestion` tool, not with prose.** Put the reasoning, evidence and
  trade-offs in the message text above the call; the tool's `description` fields are short. Give
  2–4 options, recommendation first, labelled `(Recommended)` — the tool supplies "Other" itself,
  so you do not need an escape hatch. Never close a turn with an open-ended "take X, or do you
  want Y under some condition?": it makes the user invent alternatives you should have researched.
  If the user answers "I'll take your recommendation" more than once in a row, that is a signal
  your question format is wrong, not that they have disengaged. The tool may be deferred — load it
  with ToolSearch rather than falling back to prose.
- **Facts you look up; decisions you ask.** If the answer is in the codebase, the board, or a
  doc — go read it. Never spend the user's attention on something you could have checked.
- **This applies to what you write on the board, not only to what you ask.** A hotspot description
  is read months later as authoritative, by someone who will not re-derive it. Verify before you
  assert — "the create path does not appear to check X" is a claim, and if the check exists you
  have published a falsehood that outlives the session. If you have not looked, write the question,
  not the answer.
- **The board is the source of truth.** Every resolution lands on the board (via the prooph
  board MCP) — not only in this conversation and not only in a spec doc. A decision that exists
  only in the transcript is a decision that will be re-litigated.
- Unresolved questions become **Hotspots** on the board, not silent assumptions.

## Phase 0 — Re-check the problem

This skill runs **after** a chapter has been modeled — it is a critic, and it needs an artifact
to attack. But before grilling the model's *correctness*, spend a few questions on whether it's
a model of the right problem. A well-formed model of the wrong problem is the most expensive
failure mode here, and it is much cheaper to catch now than in Build.

Do this quickly. It's a re-check, not a requirements workshop — and it's effective precisely
because there's now a concrete model to react to instead of an abstraction. Work from the
issue/task if one was passed.

1. Who is the actor, in the business's own words? (Not "the user".)
2. What are they actually trying to accomplish, and what do they do today instead?
3. What's the observable change that means this worked?
4. What is explicitly **out** of scope for this chapter?
5. What breaks or gets worse if we ship this?

If the issue is one thin line — which is normal and correct for a parked idea — this phase is
where it becomes a real problem statement. Keep it short; this is not a requirements document.

## Phase 1 — Load the model

Resolve the chapter (use the `navigation` skill for a deeplink; ask for one if a title is
ambiguous). Then read it **completely**:

- Every slice, in order, with its type.
- Every element's full details, **and its comments** — a comment is often where the last person
  to touch the code recorded what they learned.
- Every existing GWT scenario.

### Check the payload is complete before reasoning from it

A full chapter is large, so `get_chapter` normally comes back **spooled to a file** — `result (N
characters) exceeds maximum allowed tokens`. That is the healthy case: the data is complete, just
on disk. Read it.

The unhealthy case is a `<<ccr:…>>` content-reference stub inline, produced by a compression proxy
between the client and the API. **A stub is not "no details"** — it is missing data, and no tool
expands it. One command tells the two apart:

```
grep -c '<<ccr:' <the spooled file>     # 0 means you have everything
```

If stubs are present, fetch that chapter in a fresh subagent (near-empty context clears the
compression threshold) and tell it to return the text **verbatim, unsummarized** — you are going to
append to it and write it back, so a paraphrase destroys real content. Retrying in your own context
does not work. If you still cannot read a field, **never** call `update_element` or `update_slice`
on it: both replace the whole field, so writing blind destroys prose you cannot see. Use
`append_details` / `append_description`, or leave a comment — all three are additive and safe.

### Diff the board against the code

Board status fields go stale and slice details drift from shipped contracts. Use `graphify query`
(or the project's equivalent) to check whether each slice already exists, and whether its named
types/fields match reality. Surface every mismatch in the interview. Never grill a model in the
abstract when you could grill it against the code.

### Read the chapters this one borders

A chapter that references another's entity, event, or id almost always has decisions already made
for it next door. Before asking the user anything, read the neighbouring chapter's **spec doc, its
slice details, and the comments on its elements.** Reading only the neighbour's *spec doc* does
not count — three of these four payoffs live on the board, not in the doc:

- **Resolved hotspots.** The same question raised here may already be answered there, with
  rationale the user confirmed. Re-asking wastes their attention and risks contradicting a locked
  decision.
- **Blockers this chapter cannot see.** Infrastructure a slice depends on may not exist yet, and
  the place that fact gets recorded is a comment on the *other* chapter's element, written by
  whoever hit it in Build. This is what separates "the design is complete" from "this can be
  built now" — see the gate's reporting rules.
- **The canonical element spec.** The chapter that owns an event usually holds its schema, field
  origins and stream key in the element's `details`. Write your own and you have forked the spec;
  worse, the gate's field-traceability check then passes on your prose while the real schema sits
  unread next door.
- **Existing element identity.** Match the neighbour's exact element **names**, and create shared
  elements the way that board links them, before writing descriptions. Two stickies for one event
  drift independently and produce contradictory amendments — and renaming into a match afterward
  does not merge them. Check with `search_elements` by name: it lists every copy, its chapter, its
  context and its `details` side by side.

## Phase 2 — Grill, in three lenses

Work the chapter through three passes. Don't interleave them; each one asks a different kind of
question and mixing them produces shallow coverage of all three.

### Lens 1 — Structure

Is this a legal, coherent event model? Drive the `event-modeling` skill's Validation Checklist
to completion. In particular the mistakes that recur:

- A conditional outcome of one trigger modeled as a separate chapter (it's sibling slices in
  the same chapter), or a genuinely divergent journey crammed into one chapter.
- A command with no trigger **element**. The UI or Automation must be a sticky in the slice
  before it — a trigger named only in prose in the slice details is not a trigger, and reads as
  one only to whoever just wrote the prose. prooph board enforces this: it refuses an automation
  in a slice containing events, which means a gear driving a write slice needs its own slice.
  **Read a modeling tool's validation error as the modeling rule it is, not an API quirk to work
  around** — its message usually states the correct shape outright.
- **The mirror of the above: a read model with no reader.** Walk every slice and name the UI or
  Automation element that consumes its information element. An orphan — an information sticky
  alone in a slice — passes "is it one type?" and fails the real test, so checking only the
  command direction leaves half this defect class uncaught. The usual cause is a UI component
  modeled as part of the page that opens it: a form opened by a grid page is its **own** element,
  and the no-duplicate-UI rule forbids repeating the *same* sticky, not collapsing two distinct
  components into one. Getting this wrong also mis-records the next slice's command trigger.
- An event inside an Automation slice.
- A "data-loading command" (`LoadOrders`) or a UI-interaction event (`ButtonClicked`).
- Technical events that no business person would recognize as a fact.

### Lens 2 — Scenarios

**Load the `slice-scenarios` skill and drive its checklist.** It is the canonical spec for what
a Given/When/Then scenario must contain — structure, `:::element` syntax, YAML data patterns,
the six scenario types, and the writing checklist. Do not restate its rules here or invent your
own; run it.

**This lens WRITES scenarios — it does not merely audit them.** Every rule you settle during the
interview needs its GWT before the gate runs, and you are the one who writes it, in this session,
on the board. Reading this lens as "assess the existing scenarios and report what's missing" is
the most common way to burn a round trip: the gate then fails on scenario insufficiency, the user
invokes `/slice-scenarios` by hand, and the gate passes on work you were already supposed to have
done. A decision made in Phase 2 with no scenario by Phase 4 is not a decision, it is a note.

Two things that make the scenarios worth writing:

- **Use the real incident data.** If the grilling started from a live defect, the actual ids,
  values and payloads are the best evident data you will ever have — they are concrete, they are
  true, and the regression scenario reads as a reproduction rather than an invention.
- **Add the elements your scenarios reference.** A `:::element hotspot Skipped — value is empty`
  that exists in no slice fails the skill's own "does it match the elements actually in the
  slice?" check. Create it.

The bar for this lens: **the scenarios must be sufficient for a test author who has never seen
the production code to write failing tests from them alone.** That is not hypothetical — it is
literally the next step in the pipeline. Two consequences worth pushing hard on:

- **Concrete example data, not types.** `employee: Anna` and `trackingId: track1`, never
  `employeeId: string|format:uuid`. Placeholder data hides the missing-field problems Lens 3
  exists to catch, and a test author cannot write an assertion against a type name.
- **Both success and failure**, per the skill's "Document Both Success and Failure": successful
  operation, invalid state, already-done/idempotent, permission denied, missing data. A command
  with no stated failure mode is under-specified, not infallible.

For automations, the skill's scenario types don't cover one thing — add it: what triggers it,
what it reads, its rule, **and its termination policy** (what happens on failure, and does it
retry forever?).

### Lens 3 — Information completeness

This is the lens people skip, and it's the one that stalls implementation. Run
`references/completeness-gate.md` — every field of every read model and every event must have a
traceable origin. On the board this is the "no red arrows" rule; do it explicitly here.

## Phase 3 — The four staleness traps

These are the failure modes that stop an event model being trusted, drawn from *Mastering Event
Modeling and Event Sourcing*. They are cheap to catch here and expensive later, so check them
explicitly rather than hoping they surface.

- **Read model reuse across slices.** The single most common failure, and it reintroduces the
  coupling the whole approach exists to remove: one change fans out across unrelated areas. Be
  *happy* to create a purpose-built read model even when it's 99% identical to an existing one.
  If two slices share a read model, that's a finding.
- **Fat events.** An event carries the **minimum information needed** to represent its state
  change. Redundant fields destroy the event's value as an undisputed fact and make schema
  evolution harder. Ask of every field: is this the fact, or is it convenience for one consumer?
- **Modeling infrastructure instead of information flow.** Queues, topics, webhooks, tables,
  column names, technical IDs. It makes the board unreadable to business stakeholders and
  brittle when the stack changes — and an unreadable board gets ignored, which is how it dies.
- **Impure command handlers.** A handler decides validity from its inputs and past events, and
  nothing else. External calls belong in an automation, not inside the decision.

## Phase 4 — Write it down

As decisions land, put them where the project keeps them (per the config block):

- **On the board, always** — updated element details, new slices, GWT scenarios, Hotspots for
  what's still open. This is not optional; the board is the spec.
- **An ADR** for a durable decision with rationale that will outlive this chapter. Never edit an
  Accepted ADR — supersede it.
- **A glossary/domain-model entry** for any new term (invoke `domain-modeling`).
- **The journal** for what you tried and rejected, and why.

**Add to an element with `append_details` / `append_description`** — both are atomic and safe.
Reach for `update_element(details:|description:)` only to *replace* text deliberately, since it
overwrites the whole field; if Phase 1 could not read that field, leave a comment instead of
writing it blind. `update_slice(details:)` has no append form, so send the complete corrected text.

**Slice `status` tracks implementation, not design.** The vocabulary is ordered
`draft → planned → in-progress → blocked → ready → reviewed → deployed`, so `ready` sits *after*
`in-progress` and claims the slice has shipped. A slice that just passed this gate but has no code
is **`planned`** — set every passing slice to that. Reserve `blocked` for a slice genuinely stalled
on an unresolved hotspot or unbuilt dependency; one that is merely *sequenced* behind another is
still `planned`. Inflating the status makes the board lie about the one thing it is tracking.

## Exit — the gate

**Load `references/completeness-gate.md` and work it as a literal checklist before reporting
anything, and report in its block format.** It defines the verdict; one you did not run the
checklist for is a guess with a table around it. Its check 2 — "a preceding Read slice or
Automation slice" — is what catches commands driven by nothing, the defect most likely to survive
a confident-sounding pass. Its `BUILDABLE NOW` line is a *different question* from `DESIGN
COMPLETE`; answering only the second is how a Build session meets missing infrastructure at the
worst possible moment.

- **Any slice that PASSED** → run `/prewalk` in this same session, while the context is hot,
  covering exactly those slices — **provided the code baseline is stable.** See below.
- **Any slice that FAILED** → list precisely what's missing as Hotspots on the board, and record
  it in the manifest as blocked with the hotspot that blocks it. It does not get prewalked and
  it does not get built.

### When NOT to prewalk, even on a PASS

Prewalk's entire product is board-vs-code findings against a real baseline. If that baseline is
about to move, prewalking now produces a manifest that is stale before Build reads it — and a
stale manifest is worse than none, because Build trusts it.

**Check before prewalking:** is there an in-flight branch that will change the files this chapter
touches — the persistence layer, a shared command processor, a base event type, a sibling chapter
mid-Build? If yes, **stop after the gate.** Say so explicitly, name the three-or-so ways the
baseline will change, and leave prewalk for a session that runs against merged code.

This is not hypothetical. The failure it prevents has already happened once: a prewalk finding
declared a piece of Null infrastructure missing and told Build to write it, while another branch
was landing exactly that plumbing — the "largest single unknown in the slice" turned out to be
zero work, and the manifest said the opposite for weeks.

Deferring costs less than it looks like it does. Everything the grill decided is on the board,
which is the source of truth; only the narration is lost, and that fits in a memory note. Do not
hold a session open to preserve "hot context" — if you keep working in it, the context compacts
anyway, and the thing you were preserving destroys itself in the act of being used.

**"Partially passing" describes an outcome, never a plan.** Do not propose deferring a slice
you have not tried to specify. A slice is blocked only when something *outside this session* is
missing — an unbuilt dependency, a fact only a third party has. It is **not** blocked because it
would take a lot of scenario-writing, because a parameter is unconfirmed, or because another slice
is the "real value." An unconfirmed number is one `AskUserQuestion`, and asking it is the job; a
long GWT is the deliverable, not an obstacle to it. Design the whole chapter. Every slice you skip
here becomes homework for the user at a worse exchange rate, and framing that as prudent scoping is
how a Validate session quietly under-delivers while reporting success.

If the chapter-level checks fail (narrative or agreement), stop entirely — that usually means
the slice boundaries themselves are wrong, and prewalking any of it would be premature.
