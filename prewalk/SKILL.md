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

### 1. Fingerprint the baseline

Capture `git rev-parse HEAD`, the board chapter id + deeplink, and an ISO-8601 timestamp. The
Build session uses these to detect that the model moved out from under the manifest.

### 2. Decompose the slices, in dependency order

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

Three checks turn a plausible file list into a correct one:

- **Data-flow trace per GWT** — an under-scoped file list is what forces a mid-implementation stop.
- **External-payload reality check** — types and mappings drift from reality.
- **New persistence ⇒ integration test.**

Each is defined in the completeness checklist below. Run that list before you call the manifest
done.

### 3. Record the board-vs-code diff

Slices that are already shipped despite a stale board status. Slice details that contradict a
shipped contract or a revised ADR. Schema discrepancies resolved during grilling. The Build
session must not rediscover these.

### 4. Document the guardrails

- Constraints and non-goals confirmed during grilling.
- **Known pre-existing red:** any currently-failing or flaky test, by file + test name, with the
  substitute gate to use instead. Without this, every worker and the reviewer independently
  rediscovers and triages the same failure.
- Decisions that were made and must not be re-litigated.

### 5. Preflight (only if staying in this session)

- Create the branch per the project's convention.
- Run the project's baseline suite and record whether it was green.

### 6. Write the manifest, then commit it

To `manifest_path`. **Append, don't clobber** — if the target is an existing living design doc,
add your sections and leave the rest intact.

**Then commit and push it before the session ends.** An uncommitted manifest is indistinguishable
from no manifest: the next session reads the last committed state, and everything you learned is
invisible to it no matter how good it was. This is not a formality — a chapter has already been
fully grilled, written to the board, and left uncommitted, and the next session spent most of its
budget rediscovering that the work already existed. If the project's config block names a branch
convention, the manifest commit is the first commit on that branch.

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
    - [ ] GWT — verbatim by default; if you deviate, the flagged form from Step 2 (scenario titles +
          slice deeplink + the reason + "the board is authoritative for the full text").
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

- [ ] **Committed and pushed** (Step 6).

---

## Hand-off contract

State clearly at the end:

- the manifest path
- the exact command the next session should start with (e.g.
  `/manage-chapter --manifest <path>`), so the user can paste it into a fresh context
- anything you deliberately left for the Build session to decide

**Target-file lists are hints; the GWT is the spec.** Say this in the manifest. If satisfying a
GWT requires additive plumbing beyond your listed files, the Build session is authorized to add
it — the file list being incomplete is a navigation problem, not a scope violation.
