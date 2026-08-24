#!/usr/bin/env python3
"""Check that every `:::element` reference in a chapter's slice details resolves.

Scenario references name elements by string. Nothing on the board validates them, so a rename,
a typo, or a reference to an element that was never created leaves a dangling pointer that reads
as authoritative — and sends a test-writer hunting for a sticky that does not exist.

Usage:
    audit-element-refs.py <get_chapter-json>     # the file get_chapter spooled to disk
    get_chapter ... | audit-element-refs.py -    # or piped

Exit 0 = every reference resolves. Exit 1 = at least one does not.

DANGLING is always a failure: the name matches no element anywhere in the chapter.

CROSS-SLICE is reported but not failed: the name matches an element in a *different* slice. Often
legitimate — a Given event authored by an earlier slice — but it is also how a foreign event from
another chapter sneaks in. Events owned by another chapter should be plain YAML with an
attribution line, not `:::element`: adding a sticky for them would make a read slice mixed-type
and fork an element identity that chapter owns.
"""
import collections
import json
import re
import sys

REF = re.compile(r":::element[ \t]+(\w+)[ \t]*\n[ \t]*([^\n]+)")


def load(path):
    raw = json.load(sys.stdin if path == "-" else open(path))
    # get_chapter spools as [{"type": "text", "text": "<json>"}]; accept the bare object too.
    if isinstance(raw, list):
        return json.loads(raw[0]["text"])
    return raw


def main():
    if len(sys.argv) != 2:
        print(__doc__.strip(), file=sys.stderr)
        return 2

    chapter = load(sys.argv[1])
    by_slice = collections.defaultdict(set)
    for e in chapter["elements"]:
        by_slice[e["sliceId"]].add(e["name"])
    all_names = {e["name"] for e in chapter["elements"]}

    failed = warned = 0
    for s in sorted(chapter["slices"], key=lambda x: x["index"]):
        refs = REF.findall(s.get("details") or "")
        dangling = sorted({n for _, n in refs if n not in all_names})
        cross = sorted({n for _, n in refs if n in all_names and n not in by_slice[s["id"]]})
        if dangling or cross:
            print(f"[{s['index']}] {s['label']}")
        if dangling:
            failed += 1
            print(f"    DANGLING — matches no element in this chapter: {dangling}")
        if cross:
            warned += 1
            print(f"    cross-slice — defined in another slice: {cross}")

    n = len(chapter["slices"])
    if failed:
        print(f"\nFAIL — {failed} slice(s) with dangling references across {n} slices")
        return 1
    print(f"PASS — all :::element references resolve across {n} slices"
          + (f" ({warned} slice(s) with cross-slice references, review each)" if warned else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
