---
name: prewalk
description: Bridge a hardened event model to execution. Captures the design trajectory as a structured manifest so the Build session skips reconnaissance and starts with the answers. Run at the end of a /grill-model session, while the context is still hot.
argument-hint: "Chapter title or deeplink"
---

You have just finished grilling a chapter. Before this session ends, capture the trajectory so
the implementation session can execute without re-analyzing the board and the codebase from
scratch.

**Prewalk exactly the slices that PASSED the completeness gate.** The gate is per-slice, so a
chapter is normally a mix: prewalk the passing ones and list the failing ones as blocked, each
with the hotspot that blocks it, so the Build session knows they exist and knows not to touch
them. A partially-prewalked chapter is expected and fine — slices are independent and land in
any order.

Never prewalk a slice that failed. Doing so launders an unverified assumption into an
authoritative-looking manifest, and Build pays for it. If the chapter-level checks failed
(narrative or agreement), stop entirely and go back to `/grill-model` — the slice boundaries
themselves are probably wrong.

## Why this exists

The Build session's most expensive minutes are its first ones: re-reading the board, hunting
for template files, discovering that a slice needs plumbing nobody listed. You already know all
of that right now, and you will never know it this cheaply again. Write it down.

## Load project config

Read the host project's `CLAUDE.md` for its **Event modeling config** block, which specifies:

- **`manifest_path`** — where the manifest goes. This differs by project and matters:
  - Some projects keep a *disposable* manifest per chapter (`docs/chapter-manifests/<slug>.md`).
  - Some projects **accrete into an existing living design doc** (`docs/spec/<slug>.md`). There
    you do not create a new file — you append your sections to the existing one, or create it
    from the project's spec template if it doesn't exist yet.
- **`spec_template`** (optional) — a skeleton file for new spec docs (e.g. showbook's
  `docs/spec/TEMPLATE.md`). When present, start new files from it and shape your appended
  sections to match its headings instead of improvising the manifest layout from this skill's
  prose. The template's comments say which phase writes which section.
- the branch naming convention
- the build/test commands (baseline, per-change, full)
- the repo footprint (single repo, or several)

If there's no config block, ask once and offer to write it into `CLAUDE.md`.

## Steps

### 1. Start a new worktree — a clean room, not the manifest's home

Before touching the board or the repo, create an isolated worktree for this chapter's work, using
the branch naming convention captured in the config block above. **Use the repo's own provisioner
when its config names one** — showbook's `bin/slice-worktree.java provision` copies
`.env`/`node_modules`; a bare `git worktree add` leaves a tree that will not build. If its
invocation is not discoverable, the host's `wt add <slug>` works (it provisions `.env` — verified
2026-09-25); say in the manifest which route you took.

Run **fingerprinting, file discovery and the baseline suite** from inside that worktree — those
three and no more. The point is a clean room: no stray exploration files and no other session's
uncommitted diffs can contaminate the baseline you are about to fingerprint. The manifest itself is
written and committed in the main checkout (Step 7), so this worktree is disposable the moment that
lands.

**The worktree lives outside the session workspace.** Under a `workspace-write` file sandbox the
build cannot write its `target/` there and dies on a `FileSystemException` that reads like a code
failure — run the suite with full file access, or run it in the main checkout and say which.

If every worktree route fails, create the branch normally per the project's convention and note
the fallback in the manifest so the Build session knows it isn't in a dedicated worktree.

### 2. Fingerprint the baseline

Capture `git rev-parse HEAD`, the board chapter id + deeplink, and an ISO-8601 timestamp **read from
`date -u`, never inferred** — a wrong date silently mis-orders decisions against the ones already on
the board. The Build session uses these to detect that the model moved out from under the manifest.

`git rev-parse HEAD` is also worth comparing against the main checkout: the default branch may have
moved while the grill ran, and the manifest must name the commit it was actually prewalked against.

### 3. Decompose the slices, in dependency order

A read model / view comes after the command that emits its events. A UI slice comes after the
route it calls. For each slice record its GWT (verbatim from the board — do not paraphrase; the
board's wording is the spec) and its target files.

**When to reference the board instead of copying it.** Verbatim is the default because the Build
session should not have to go looking. But copying creates a second copy, and two copies drift —
which contradicts "the board is the source of truth" the moment someone edits one of them. Copy
less when *both* of these hold:

- the board slice details were written or amended in **this same session**, so they cannot already
  be stale relative to your understanding, and
- the full GWT text is large enough that duplicating it makes the manifest harder to act on
  (a rough line: more than a page or two per slice).

In that case, record per slice: the **scenario titles**, the **slice deeplink**, and the one or
two scenarios **verbatim** that encode a live defect or a regression you must not lose. Then say
plainly in the manifest that the board is authoritative for the full text, and that you deviated
deliberately and why. An unflagged partial copy is the failure mode — it reads as complete.

**A slice with no scenarios of its own is not buildable. Do not give it a `TODO` marker.**

If you find yourself writing "no scenarios of its own — its behavior is exercised by slice N's", or
"contract only, not a decision", or "wiring now, body filled in later", you have found a slice with
no observable behavior. It cannot be tested honestly on its own: the class it produces will have no
collaborator, so `createNull()` would be identical to `create()`, and the test-writer's only way to
observe anything is to boot Spring and spy on a subclass. That is scaffolding around a stub, and it
passes review looking like a well-written test.

Resolve it here, one of two ways:

- **Merge it** into the slice whose scenarios exercise it — they are one unit of behavior that the
  model happened to draw as two boxes. This is not a delivery unit (which shares a PR across
  slices that each stand alone); it is recognising a boundary that was never real.
- **Flag it** in the manifest as needing a design call before Build, and say what is missing.

**A third case looks similar and is not a defect.** A slice the gate passed that carries **no work
in this unit** — an earlier amendment already built it and this one only *checked* it, which the
board says in those words ("checked, nothing changes here"). Give it an explicit
`NO-OP (verified)` marker and the reason, never a `TODO`: the failure this prevents is a Build
session inventing a stub so it has something to do.

Note the failure this prevents is *not* a slice being small. It is a slice being **empty of
decisions**. A one-line slice with a real GWT is fine.

*Incident:* an earlier readiness manifest wrote "No scenarios of its own by design" and still carried
a `TODO`; Build produced a stub class and a 220-line `@SpringBootTest` with a spy subclass, and a
reviewer passed it (PR #396). The disqualifying sentence was already in the manifest.

Three checks turn a plausible file list into a correct one:

- **Data-flow trace per GWT** — an under-scoped file list is what forces a mid-implementation stop.
- **External-payload reality check** — types and mappings drift from reality.
- **New persistence ⇒ integration test.**

Each is defined in the completeness checklist below. Run that list before you call the manifest
done.

**Write the done contract, per slice, before Build starts.**

For each slice, record the checks that will decide it is finished — while the design is still hot
and before any code exists:

| Outcome | Proof command | Passes when |
|---|---|---|
| <the one thing that must become true> | <command> | <the exact result that settles it> |

Four rules make this worth the lines it costs:

- **Only slice-specific gates.** The project's standing gates — suite green, format, lint — already
  run on every slice, and repeating them here is noise that buries the signal. Record what is true
  of *this* slice and nothing else: the scenario that must become observable, the migration that
  must apply, the arch-test that must now pass, the route that must answer.
- **Every row needs a command a machine runs and a result a machine can see.** "Handles the empty
  case" is not a gate; `./mvnw test -Dtest=FooTest#rejectsPastDate` is.
- **One row per GWT scenario, at minimum.** A scenario with no row is a scenario nobody committed
  to proving.
- **Evidence is not recorded here.** Build pastes the actual command output into the PR body
  against each row. A ticked row with no output is unmet — worse than an unticked one, which is at
  least honest about where the work stopped.

The point is *not* to catch a worker lying about a test run; the suite already does that, and it
does it better than a checklist can. The point is that the contract is written **before**
implementation, so a slice that quietly got smaller shows up as a row nobody could fill in —
instead of disappearing into an end-of-session summary that only describes what was built.

**Group qualifying threads into delivery units.** When consecutive slices carry **one additive
contract forward through the chapter** — a new event type threaded from its definition, through
the write path, to every read side that renders it — record the grouping in a `## Delivery units`
manifest section, with a why-it-qualifies line per group. Build never groups on its own judgment:
absent the section, every slice is its own unit. The universal shape is: at most three consecutive
slices; roughly ten production files across the group; one contract per group; nothing pending (no
open hotspot or unresolved design question). The boundary rules are the host project's — for
showbook, ADR-0086 and `.dsh/skills/manage-chapter/references/delivery-units.md`; read them there
rather than here. The per-slice sub-loop is unchanged — the unit shares only the branch, the
standing gates, the PR, the review round and the land round.

### 4. Record the board-vs-code diff

Slices that are already shipped despite a stale board status. Slice details that contradict a
shipped contract or a revised ADR. Schema discrepancies resolved during grilling. The Build
session must not rediscover these.

### 5. Document the guardrails

- Constraints and non-goals confirmed during grilling.
- **Known pre-existing red:** any currently-failing or flaky test, by file + test name, with the
  substitute gate to use instead. Without this, every worker and the reviewer independently
  rediscovers and triages the same failure.
- Decisions that were made and must not be re-litigated.

### 6. Preflight

Run the project's baseline suite from inside the worktree created in Step 1 and record whether
it was green. **Before characterising or publishing a red, check whether a fix is already in
flight** (`git fetch` + `gh pr list --state open`) — in a repo with other writers a red baseline is
often being fixed right now, and a prewalk published against a red that the next commit removes
costs a corrective manifest commit and a whole suite run. Name the exact failing tests so the
substitute gate is usable.

### 7. Write the manifest in the MAIN CHECKOUT, and commit it there

**Write and commit the manifest from the main checkout on the default branch — not in Step 1's
worktree.** The manifest is a document *every* later branch needs, and Build cuts its branch per
slice from the default branch, so writing it where it must end up removes the cross-branch transfer
and its whole class of silent failure. It is a feature branch's job to hold work under review; the
manifest is not under review, it is the input to the work. **Unless the host's parallel-writer rule
says otherwise** — showbook sends any session that is not the sole writer into a `wt` worktree, and a
prewalk's board/docs writes count: land it from there instead, and confirm the manifest is on the
default branch before Step 8.

To `manifest_path`. **Append, don't clobber** — if the target is an existing living design doc,
add your sections and leave the rest intact.

**Then commit and push before the session ends.** An uncommitted manifest is indistinguishable from
no manifest: the next session reads the last committed state. Not a formality — a chapter has been
fully grilled, written to the board, and left uncommitted, and the next session spent most of its
budget rediscovering work that already existed.

**Verify by reading it back rather than assuming.** The failure is silent, not loud: if a pre-grill
version already exists at `manifest_path` — the normal case for a living design doc — Build reads
*that* and never errors. A stale plan looks exactly like a fresh one.

```
git log --oneline -1 -- <manifest_path>
grep -c "<a heading you just wrote>" <manifest_path>          # expect 1, not 0
git rev-parse HEAD && git rev-parse origin/<default-branch>   # must match
```

Landed from a `wt` worktree (the parallel-writer path above)? The shared checkout's HEAD
intentionally does not move, so those `rev-parse`s will disagree — that is not a failure. Verify
against the default branch instead: `git fetch && git merge-base --is-ancestor <commit> origin/<default-branch>`,
and read the manifest back with `git show origin/<default-branch>:<manifest_path>`.

If the project requires a PR to its default branch, open one for the manifest commit and **say in
the hand-off that Build is blocked until it merges.** Never report the prewalk as finished with
the manifest unmerged.

### 8. Finish the dispatch handoff — when the host config defines a machine trigger

Some hosts wire prewalk's output straight into an automated dispatcher: the project's event-modeling
config names a **machine trigger** — typically a label plus a pointer line in the work item's body
that the dispatcher parses. When the config defines one, finish the handoff so the pointer parses:

- **Add or repair the pointer line** in the chapter's work item body, byte-exact per the host
  config. Idempotent: absent → add it; present but malformed → rewrite it; correct → touch nothing.
  A dispatcher reads the body mechanically — a pointer embedded mid-sentence files a WorkItem whose
  intake examines each work item exactly once, so the fail-closed hold then lasts forever.
- **Order is load-bearing:** the manifest must already be committed and pushed to the default
  branch (Step 7) before the pointer exists. The pointer is a promise that the file is there.
- **Verify by reading the body back**, not by trusting the edit.

**Who applies the dispatch label is the host config's call.** Applying it enqueues automated builds,
so the default is to stop and hand it over: report the work item as **dispatch-ready** — pointer
verified, manifest on the default branch — and leave the label to the human. A host whose
event-modeling config instead names the session as the applier wants you to apply it yourself, once
this step's completion criterion holds; take that from the config, and never apply the label before
the criterion does.

Completion criterion: the body carries the pointer in the host's exact format, read back and
confirmed, with the manifest it names on the default branch — and the dispatch label applied by
whoever the host config names, and only once that criterion holds.

**Prove the dispatcher resolves *this* slice before reporting dispatch-ready.** The pointer and the
label promise a file is there, not that the dispatcher can read it: run whatever picks the target
(the host's own parser or picker script) and confirm it names your slice. A pointed-at, labelled
manifest whose slice list and markers disagree with the slice you just added resolves to an older
one (2026-09-25: Capataz's preflight held fail-closed on a slice merged six weeks earlier).

---

## Manifest content — the completeness checklist

Adapt the headings to the host project's doc conventions, but every line below must be present and
answerable. **Run this list explicitly before you call the manifest done.** A manifest written to
the headings alone is the known failure mode — it reads as authoritative and complete while missing
the section Build actually opens it for.

- [ ] **Front matter / identity** — chapter id, title, board deeplink, created-at, base commit,
      suggested branch, repo footprint, preflight state (baseline green or not).

- [ ] **Slices (execution graph)** — numbered, in dependency order, each with:
    - [ ] **Target files, production AND test, by path.** ← *This is the deliverable.* Everything
          else in the manifest is context for it. A slice entry without a file list has not been
          prewalked, however much prose surrounds it. If the project has a template or an
          already-shipped sibling to mirror, name the specific file per target.
    - [ ] status marker (`TODO` / `IN PROGRESS` / `DONE` / `MERGED`) the Build session can tick.
          **A slice with no scenarios of its own never gets `TODO`** — merge it, flag it for a
          design call, or mark it `NO-OP (verified)` if it carries no work in this unit (Step 3).
          Writing "no scenarios by design" next to a buildable marker is the contradiction this gate
          exists to catch.
    - [ ] GWT — verbatim by default; if you deviate, the flagged form from Step 3 (scenario titles +
          slice deeplink + the reason + "the board is authoritative for the full text").
    - [ ] **Done contract** — the slice-specific proof table (Step 3): outcome, proof command,
          passes-when, one row per GWT scenario at minimum. Standing project gates excluded.
    - [ ] any board-vs-code mismatch specific to this slice.
    - [ ] anything the slice needs that isn't a slice file — arch-test allowlists, menu registration,
          config, a shared type another slice will also want.

- [ ] **Data-flow trace per GWT** — for every comparison or decision a GWT specifies, verify that
      *all* of its inputs actually reach the named code seam **today**, and say so. A
      "most-recent-wins" rule needs both sides' timestamps at the compare site — go look. If an
      input doesn't reach it, the plumbing files (types, mappings, fetches) go in Target Files
      explicitly.

- [ ] **External-payload reality check** — for any slice reading fields from an external API or file
      format, confirm the field names against a captured real payload or a live probe, and record
      the real shape. Write "N/A" and why if none.

- [ ] **New persistence ⇒ integration test** — any slice adding a real-store-backed repository lists
      a real integration test (name the project's exemplar) and any migration in Target Files. If
      you conclude none is needed, **say so and give the reason**; a silent absence is
      indistinguishable from an oversight.

- [ ] **Board-vs-code findings** — the gap between model and reality, including slices already
      shipped despite a stale board status, and any earlier findings this pass supersedes.

- [ ] **Guardrails & non-goals** — constraints, known pre-existing red (file + test name + substitute
      gate), explicit out-of-scope.

- [ ] **Approved decisions** — what was settled during grilling, *with the why*. A decision without
      its rationale gets overturned by the next plausible-sounding argument.

- [ ] **"Target-file lists are hints; the GWT is the spec"** — stated in the manifest, so Build knows
      it is authorized to add plumbing beyond the list.

- [ ] **Committed, pushed, and landed on the default branch** (Step 7) — verified by reading
      `manifest_path` from the main checkout, not from the worktree you wrote it in.

---

## Hand-off contract

State clearly at the end:

- the manifest path, **and the commit on the default branch that carries it** (Step 7)
- the **dispatch handoff**: pointer line written and verified (Step 8), the work item
  **dispatch-ready**, and who applied the dispatch label — the human where the host config is silent,
  the session itself where the config names it
- that the next session runs **from the main checkout, not from a worktree.** Build creates its
  own branch or worktree per slice and expects to be invoked from the default branch. Say plainly
  that Step 1's worktree is now disposable, and name the branch and folder so they can be cleaned
  up.
- the exact command the next session should start with (e.g.
  `/manage-chapter --manifest <path>`), so the user can paste it into a fresh context
- anything you deliberately left for the Build session to decide

**Target-file lists are hints; the GWT is the spec.** Say this in the manifest. If satisfying a
GWT requires additive plumbing beyond your listed files, the Build session is authorized to add
it — the file list being incomplete is a navigation problem, not a scope violation.
