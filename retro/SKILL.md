---
name: retro
description: End-of-session reflection that improves the skills, agents, and references actually used this session - prunes them - files work a script should own and scripts that should change as issues for the host's dev cycle, with the tracker fields its rules require - leaves breadcrumbs for the next session (tasks filed, session record entry, edits committed) - and audits the always-loaded steering files against their context budget. Run as the last step of a manager loop, or standalone when a session ends. Biased toward deletion; instruction files must not grow monotonically.
argument-hint: "(optional) what to focus on"
disable-model-invocation: true
---

Reflect on the work done in this session: how can the skills, agents, commands, and references
used be improved — and what work should have been a script?

Then **make the changes.** A retro that only reports findings is a retro whose findings evaporate.

## The one rule that matters

**Instruction files must not grow monotonically.** Every retro adds; almost none subtract; after
twenty sessions the skill that was sharp is a wall of caveats nobody reads — including you, since
a 400-line SKILL.md competes with the actual task for attention. Long instructions are not more
authoritative, they're less.

So this skill is **biased toward deletion**. Before proposing any addition, find something to
remove. If you genuinely can't, say so explicitly rather than skipping the question.

## What qualifies as a finding

Only **evidence from this session** — read the transcript rather than your memory of it (step 2).

- ✅ A worker misread an instruction, so you had to correct it.
- ✅ You or a worker searched for something that should have been stated up front. The fix is a
  **navigation pointer** where the search should have started, not a rule restating the fact.
- ✅ A step was ambiguous and you had to guess which reading was meant.
- ✅ A gate passed that shouldn't have, or fired when it shouldn't have.
- ✅ An instruction contradicted another instruction.
- ✅ An instruction told you to use a tool, path, or agent that doesn't exist.
- ✅ A mistake an automated check could have caught. Read the repo's own check command first
  (`package.json`/build-tool `lint`/`check` scripts, the CI workflow) — a check that already
  exists but sits unwired or silently broken is the finding, not a reinvention. And a repo with
  **no guardrail at all** (no pre-commit hook and no CI job running its lint/typecheck/test
  command) is a standing finding, not a neutral default.
- ✅ An expensive tool call a cheaper one would have covered — an MCP query returning thousands
  of tokens for one field, a broad grep where a doc pointer existed. Fix the tool or the query,
  not the instructions.
- ✅ A crucial piece of information the agent couldn't see — server output nobody teed, a
  third-party service with no readable surface. Fix the plumbing (tee a log, expose a tool, add
  a standing fact), not the instructions.
- ✅ A file, section, or rule was never consulted and nothing was lost by that.
- ✅ Mechanical work you did by hand that a command could prove — a crank a script should own —
  or a script you ran that was friction: wrong output you patched by hand, an awkward invocation,
  an error that said nothing. Both are **script work** (a suggestion, a change, or a removal),
  not an instruction-file edit. Evidence bar: it happened this session; worth bar: it recurs, or
  it's costly enough that a one-off pays. Retro specs it and files it (step 5b); the build loop builds.

Not findings:

- ❌ Things that worked. Do not add a rule reinforcing something that already went fine.
- ❌ Hypothetical failures nobody hit. Speculative hardening is how these files bloat.
- ❌ General best practices not specific to this project's actual friction.
- ❌ Restating a rule that's already stated elsewhere in the same file.

**One real friction is worth more than five plausible improvements.** If the session was clean,
the correct retro output is "clean run, no changes" plus any pruning. Say that and stop —
inventing findings to look thorough is the failure mode here. The one exception is the
always-loaded tier: its cost was paid this session whether or not anything went wrong.

## Process

### 1. Inventory what was actually used

List the skills, agents, references, config, and scripts the session touched. You only have
standing to edit what you used — a file you didn't exercise, you can't judge.

Then size the always-loaded tier: `wc -c ~/.dsh/AGENTS.md <repo>/AGENTS.md` (plus any `CLAUDE.md`
that is a distinct file), against the byte budget the DSH profile's `agent-instructions` row sets
(65536 here, 2026-10-05). Every session *and every subagent* pays for that tier, and past the
budget it does not degrade gracefully: the renderer omits the least specific file first, leaving
one line — `Workspace instruction budget N bytes: omitted <path>` — so the global file's rules
silently stop reaching every session. A tier near the budget, or that marker in your own context,
is the highest-severity finding a retro can produce, and it needs no friction to justify.

### 2. Find the frictions

Read the primary source rather than reconstructing it from memory: the session is
`$DSH_SESSION_ID` (already in the shell env — no directory-walking), so the transcript is
`~/.dsh/sessions/<workspace-slug>/$DSH_SESSION_ID/session.v4.jsonl.zstd`, zstd JSONL whose top-level
`type` is `tool/call`, `tool/result` or `assistant/message` (only the last version number is live; a
`v3` file beside a `v4` is stale). A worker's own transcript holds that worker's side of the friction. Then walk chronologically: at each point
where you corrected a worker, re-read an instruction, searched for something that should have
been given, or hit a surprise — record what happened and which file should have prevented it.
Include friction *you* caused: a manager that forgot a step is evidence the step is in the wrong
place or badly signposted.

Alongside the frictions, spot the cranks: work that was *mechanical* rather than judged — a
transform, a search, a reformat, a gate re-run by hand. If a command could prove it, it's script
work — spec it for filing (step 5b), the same as a script that was itself friction.

**Fix the phase that caused it, not the phase that hit it.** A worker escalating "the spec is
under-specified" is evidence about the *upstream* step that produced the spec, not about the
worker — the worker did the right thing by refusing to invent one. Hardening the worker in that
case makes things worse: it teaches it to guess. Trace each friction to where the information
should have been created and fix it there. Corollary: enforcement belongs where context is
cheapest — a rule the reviewer can check on a diff beats one the implementer must remember
mid-exploration.

Classify a standards violation before you fix it. A **mechanical** one — a fixed syntactic
pattern, a banned API, an import shape, a file-location rule — gets a deterministic check, full
stop: a rule in the repo's own linter, a pre-commit hook, or a CI job, whichever is cheapest
here, and that check is **script work** filed per step 5b, not a sentence in
`CODING_STANDARDS.md`. Reserve the standards file for genuine **judgement calls** — cross-file
consistency, "matches the surrounding style" — that no check could ever substitute for. Default
to building the check over writing the rule.

### 3. Hunt for cuts — before writing any additions

For each file used:

- **Dead references.** Does it name an agent, skill, path, command, or file that doesn't exist?
  (Verify — don't assume it exists because it's written down.) Delete or fix.
- **Dead weight.** A section you skipped and lost nothing by; a conditional that has never been
  true; a line that changes no behavior — "be careful", "prefer clarity", a practice the model
  already follows; an old workflow or tool; an example pinned to a merged issue or a deleted file,
  which invites pattern-matching on something no longer real. Read by every agent, moving none.
- **The wrong tier.** Where a line lives is what it costs: the always-loaded files sit in the
  context window of every session *and every subagent*; `CODING_STANDARDS.md` is read at review
  time; docs and skills on demand, a skill costing only its description line. A review-only rule
  belongs in the standards file, a fact in docs, the steering file keeps the pointer. Demoting a
  paragraph is the cheapest token saved, and it changes nothing about the rule.
- **Duplication.** Is a rule stated in two files? Keep it in the more specific one, delete the
  other, and cross-reference if needed.
- **Machine-owned text.** A marked block (`<!-- ponytail-rules:start -->` … `:end -->`) is
  regenerated from a source repo by a sync script — hand edits there are lost, then reappear.
  Fix the source, or leave it alone and say so.

### 4. Make the changes

Apply the cuts and the additions. Preferences:

- **Prefer editing over appending.** If a rule was misread, sharpen the existing sentence rather
  than adding a clarifying one next to it — you're amending, not rewriting. Two sentences on one
  topic is how contradictions form.
- **Prefer specific over general.** "Confirm a `Tests run:` count" beats "be careful with tests."
- **Put the rule where it fires.** A constraint the worker needs belongs in the worker's file,
  not only in the manager's — workers don't read the manager's instructions.
- **State the why for anything non-obvious.** A rule whose reason isn't given gets deleted by a
  future retro that can't see the point of it. One clause is enough.

### 5. Report

```
RETRO

Used:      <files touched>
Frictions: <n>  (or "clean run")
Script work: <n> — new|change|removal, filed as <#N, …> (omit when zero)
Removed:   <path> — <what and why>
Changed:   <path> — <what and why>
Added:     <path> — <what and why, and what was cut to make room>
Breadcrumbs: <tasks filed · session record path> (omit when zero)
Net:       <+/- lines across all instruction files>
Always-on: <bytes in the always-loaded tier, of the budget> (omit when unchanged)
```

Each script suggestion or script change is an issue body, filed per step 5b:

```
### `bin/<name>` — new
Replaced: <what you did by hand this session, and when>
Contract: <inputs → outputs>
Gate: <the command that proves it — test, formatter, diff>
Wiring: <skill/agent file(s) + section to amend so the script gets called — the PR wires it in>
Example: <a small runnable sketch>
```

```
### `bin/<name>` — modify | improve | remove
Friction: <what the script did wrong this session, and when>
Contract: <inputs → outputs; what must stay the same for its callers>
Gate: <the command that proves the change — re-run the script's own gate>
```

### 5b. File script work as issues for the host's dev cycle

A spec that lives only in the retro report rots in a transcript. File each script suggestion and
script change as an issue in the session's repo tracker (REST `gh api repos/<owner>/<repo>/issues`),
body = the spec above — **with the native fields the host's own tracker rules require.** The repo's
`AGENTS.md` beats this skill's defaults on label vocabulary, and it may treat a milestone or a
project column as what makes an issue visible at all — showbook's daily driver orders only by
milestone due date. An issue the host's tracker cannot see is an issue nobody works. For a **new**
script, the issue's **Wiring** field names the skills/agent files that must learn to call it — an
unwired script is dead code, so the implementer's PR amends those files in the same change. The
report points at the issue numbers instead of carrying the specs.

**If Net is positive, justify it in one line.** Growth is allowed — the files aren't finished —
but it should be a decision, not an accident.

### 6. Route what doesn't belong in a skill file

- A durable decision with rationale → an ADR in the project's `adr_path`.
- A narrative of what was tried and learned → the project's `journal_path`.
- A fact about the domain → the glossary, via `domain-modeling`.
- A learning about the *system being built* rather than the *process* → the event model, as an
  implementation note on the relevant board element.

Skill files are for how to do the work; knowledge that belongs elsewhere is the other way these
files bloat.

### 7. Leave breadcrumbs for the next session

Skill edits improve future sessions but record none of this one. Before closing:

- **Open tasks → the tracker.** Anything this session left undone that outlives it gets filed as
  an issue — next steps never live only in conversation.
- **One breadcrumb entry** in the project's session record — `runs/<id>.md`, `journal_path`,
  whatever the project keeps: date, one or two sentences of what happened and why, a pointer to
  the main artifact. Not a handoff document (`/handoff` writes one for a fresh agent) and not a
  resume manifest (the pause bookmark) — durable state for this project's next session.
- **Commit the edits.** Retro's own changes — skill files, docs, entries — land before the
  session ends, not as a dirty tree the next session discovers.

## Scope

Default to the **project-local** files under `.claude/`. Only touch global files
(`~/.agents/skills/`, `~/AGENTS.md`) when the friction is genuinely project-independent — and
say so explicitly when you do, since it affects every other project.

Skill files usually live **outside** the session workspace (`~/.dsh/skills/*` symlink into their own
repos), so a `workspace-write` sandbox denies the edits: take one escalation for the edits *and* the
commit, or run the retro in a full-access session — never report findings instead of making them.

When invoked with an explicit target (`/retro ~/.agents/skills/foo`), that target is in scope
regardless of where it lives, and the pruning hunt in step 3 applies to it in full.

**This skill owns skill edits.** Other skills hand their session learnings here rather than
carrying their own reflection step — one mechanism, and the only one biased toward deletion.

## Upstream

Fused 2026-10-05 with Matt Pocock's `retro` (`github.com/mattpocock/skills`,
`skills/engineering/retro`, upstream `a7d038f6bf`). Taken: the transcript as the primary source; a
mechanical violation gets a deterministic check rather than a rule; an absent guardrail is itself
a finding; and auditing the always-loaded steering files, deepened here into the measured byte
budget above. Rejected: his propose-only ending (presenting candidates is how findings evaporate)
and his environment-only scope, which has no home for script work or tracker filing. **Do not
install his over this one** — `~/.agents/skills/retro` symlinks here, so a clobber shows as a
dirty file; re-fuse the ideas instead.
