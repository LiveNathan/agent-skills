---
name: retro
description: End-of-session reflection that improves the skills, agents, and references actually used this session - prunes them - flags work a script should own and scripts that should change - and leaves breadcrumbs for the next session (tasks filed, session record entry, edits committed). Run as the last step of a manager loop, or standalone when a session ends. Biased toward deletion; instruction files must not grow monotonically.
argument-hint: "(optional) what to focus on"
disable-model-invocation: true
---

Reflect on the work done in this session: how can the skills, agents, commands, and references
used be improved — and what work should have been a script — for less friction and more robust,
efficient work next time?

Then **make the changes.** A retro that only reports findings is a retro whose findings evaporate.

## The one rule that matters

**Instruction files must not grow monotonically.** Every retro adds; almost none subtract; after
twenty sessions the skill that was sharp is a wall of caveats nobody reads — including you, since
a 400-line SKILL.md competes with the actual task for attention. Long instructions are not more
authoritative, they're less.

So this skill is **biased toward deletion**. Before proposing any addition, find something to
remove. If you genuinely can't, say so explicitly rather than skipping the question.

## What qualifies as a finding

Only **evidence from this session**. You watched the work happen; use what you saw.

- ✅ A worker misread an instruction, so you had to correct it.
- ✅ You or a worker searched for something that should have been stated up front.
- ✅ A step was ambiguous and you had to guess which reading was meant.
- ✅ A gate passed that shouldn't have, or fired when it shouldn't have.
- ✅ An instruction contradicted another instruction.
- ✅ An instruction told you to use a tool, path, or agent that doesn't exist.
- ✅ An expensive tool call a cheaper one would have covered — an MCP query returning thousands
  of tokens for one field, a broad grep where a doc pointer existed. Fix the tool or the query,
  not the instructions.
- ✅ A crucial piece of information the agent couldn't see — server output nobody teed, a
  third-party service with no readable surface. Fix the plumbing (tee a log, expose a tool, add
  a standing fact), not the instructions.
- ✅ A file, section, or rule was never consulted and nothing was lost by that.
- ✅ Mechanical work you did by hand that a command could prove — a crank a script should own.
  That's a **script suggestion**, not a friction. Evidence bar: it happened this session; worth
  bar: it recurs, or it's costly enough that a one-off pays. Retro suggests; the build loop builds.
- ✅ A script you ran that was friction — wrong output you patched by hand, an awkward invocation,
  an error that said nothing. That's a **script change** (modify, improve, or remove), not a
  friction. Same evidence bar as a script suggestion; retro specs it, the build loop applies it.

Not findings:

- ❌ Things that worked. Do not add a rule reinforcing something that already went fine.
- ❌ Hypothetical failures nobody hit. Speculative hardening is how these files bloat.
- ❌ General best practices not specific to this project's actual friction.
- ❌ Restating a rule that's already stated elsewhere in the same file.

**One real friction is worth more than five plausible improvements.** If the session was clean,
the correct retro output is "clean run, no changes" plus any pruning. Say that and stop —
inventing findings to look thorough is the failure mode here.

## Process

### 1. Inventory what was actually used

List the skills, agents, references, config, and scripts the session touched. You only have
standing to edit what you used — a file you didn't exercise, you can't judge.

### 2. Find the frictions

Walk the session chronologically. At each point where you corrected a worker, re-read an
instruction, searched for something that should have been given, or hit a surprise: record what
happened and which file should have prevented it.

Include friction *you* caused. A manager that forgot a step is evidence the step is in the wrong
place or badly signposted.

Alongside the frictions, spot the cranks: work that was *mechanical* rather than judged — a
transform, a search, a reformat, a gate re-run by hand. If a command could prove it, it's a script
suggestion — name it for the report (step 5).

Then audit the scripts you actually ran the same way — a script that was friction is a **script
change** (modify, improve, or remove), named for the report (step 5).

**Fix the phase that caused it, not the phase that hit it.** A worker escalating "the spec is
under-specified" is evidence about the *upstream* step that produced the spec, not about the
worker — the worker did the right thing by refusing to invent one. Hardening the worker in that
case makes things worse: it teaches it to guess. Trace each friction to where the information
should have been created and fix it there. Corollary: enforcement belongs where context is
cheapest — a rule the reviewer can check on a diff beats one the implementer must remember
mid-exploration.

### 3. Hunt for cuts — before writing any additions

For each file used:

- **Dead references.** Does it name an agent, skill, path, command, or file that doesn't exist?
  (Verify — don't assume it exists because it's written down.) Delete or fix.
- **Never-consulted sections.** Did you skip a section entirely and lose nothing? Candidate.
- **Duplication.** Is a rule stated in two files? Keep it in the more specific one, delete the
  other, and cross-reference if needed.
- **Superseded rules.** Does it describe an old workflow, an old tool, an old agent topology?
- **Rules that never fire.** A conditional whose condition has never been true is speculative
  weight.
- **Stale examples.** Examples referencing merged issues or deleted files are worse than none —
  they invite pattern-matching on something no longer real.

### 4. Make the changes

Apply the cuts and the additions. Preferences:

- **Prefer editing over appending.** If a rule was misread, sharpen the existing sentence rather
  than adding a clarifying one next to it. Two sentences on one topic is how contradictions form.
- **Prefer specific over general.** "Confirm a `Tests run:` count" beats "be careful with tests."
- **Put the rule where it fires.** A constraint the worker needs belongs in the worker's file,
  not only in the manager's — workers don't read the manager's instructions.
- **State the why for anything non-obvious.** A rule whose reason isn't given gets deleted by a
  future retro that can't see the point of it. One clause is enough.
- **Keep the file's existing voice and structure.** You're amending, not rewriting.

### 5. Report

```
RETRO

Used:      <files touched>
Frictions: <n>  (or "clean run")
Scriptable: <n> — new-script specs below (omit when zero)
Script changes: <n> — modify/improve/remove specs below (omit when zero)
Removed:   <path> — <what and why>
Changed:   <path> — <what and why>
Added:     <path> — <what and why, and what was cut to make room>
Breadcrumbs: <tasks filed · session record path> (omit when zero)
Net:       <+/- lines across all instruction files>
```

Each script suggestion or script change is a spec you can copy into a build session:

```
### `bin/<name>` — new
Replaced: <what you did by hand this session, and when>
Contract: <inputs → outputs>
Gate: <the command that proves it — test, formatter, diff>
Example: <a small runnable sketch>
```

```
### `bin/<name>` — modify | improve | remove
Friction: <what the script did wrong this session, and when>
Contract: <inputs → outputs; what must stay the same for its callers>
Gate: <the command that proves the change — re-run the script's own gate>
```

**If Net is positive, justify it in one line.** Growth is allowed — the files aren't finished —
but it should be a decision, not an accident.

### 6. Route what doesn't belong in a skill file

Not every learning is an instruction-file edit:

- A durable decision with rationale → an ADR in the project's `adr_path`.
- A narrative of what was tried and learned → the project's `journal_path`.
- A fact about the domain → the glossary, via `domain-modeling`.
- A learning about the *system being built* rather than the *process* → the event model, as an
  implementation note on the relevant board element.

Skill files are for how to do the work. Don't let them absorb knowledge that belongs elsewhere —
that's the other way these files bloat.

### 7. Leave breadcrumbs for the next session

Skill edits improve future sessions but record none of this one. Before closing:

- **Open tasks → the tracker.** Anything this session left undone that outlives it gets filed as
  an issue or added to the project's task list. Next steps never live only in conversation.
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

When invoked with an explicit target (`/retro ~/.agents/skills/foo`), that target is in scope
regardless of where it lives, and the pruning hunt in step 3 applies to it in full.

**This skill owns skill edits.** Other skills hand their session learnings here rather than
carrying their own reflection step — one mechanism, and the only one biased toward deletion.
