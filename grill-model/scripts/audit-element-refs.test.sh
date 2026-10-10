#!/usr/bin/env bash
# Self-check for audit-element-refs.java.  Run: bash grill-model/scripts/audit-element-refs.test.sh
#
# The 2026-10-09 defect: the pattern matched only the fenced form, so inline references were
# invisible and the script printed PASS without checking them. Fixture 1 pins the regression — an
# inline reference to a name that resolves nowhere must exit 1, be named as DANGLING, and be flagged
# UNVERIFIABLE (the form is not documented board syntax).
set -u
cd "$(dirname "$0")"
script=audit-element-refs.java
fail=0

run() { java "$script" "testdata/$1" 2>&1; }

# 1. Both forms in one slice: the fenced ref resolves, the inline ref does not → exit 1, name
#    reported, inline form flagged, and both references counted.
out=$(run inline-dangling.json); code=$?
if [ "$code" -ne 1 ]; then
  echo "FAIL  inline-dangling: exit $code, wanted 1"; echo "$out"; fail=1
elif ! grep -qF 'Ghost Element' <<<"$out"; then
  echo "FAIL  inline-dangling: dangling name not reported"; echo "$out"; fail=1
elif ! grep -qF 'DANGLING' <<<"$out"; then
  echo "FAIL  inline-dangling: no DANGLING verdict"; echo "$out"; fail=1
elif ! grep -qF 'UNVERIFIABLE' <<<"$out"; then
  echo "FAIL  inline-dangling: inline form not flagged UNVERIFIABLE"; echo "$out"; fail=1
elif ! grep -qF '2 references checked' <<<"$out" || ! grep -qF '1 fenced, 1 inline' <<<"$out"; then
  echo "FAIL  inline-dangling: both forms not both counted"; echo "$out"; fail=1
else
  echo "ok    inline-dangling: exit 1, 'Ghost Element' DANGLING, inline UNVERIFIABLE, 1 fenced + 1 inline counted"
fi

# 2. Fenced references only → exit 0, count and split reported.
out=$(run fenced-only.json); code=$?
if [ "$code" -ne 0 ]; then
  echo "FAIL  fenced-only: exit $code, wanted 0"; echo "$out"; fail=1
elif ! grep -qF '1 reference checked' <<<"$out" || ! grep -qF '1 fenced, 0 inline' <<<"$out"; then
  echo "FAIL  fenced-only: count/split not reported"; echo "$out"; fail=1
else
  echo "ok    fenced-only: exit 0, '1 reference checked ... 1 fenced, 0 inline'"
fi

# 3. Zero references → exit 0 and a visibly zero count, never silence.
out=$(run empty.json); code=$?
if [ "$code" -ne 0 ]; then
  echo "FAIL  empty: exit $code, wanted 0"; echo "$out"; fail=1
elif ! grep -qF '0 references checked' <<<"$out"; then
  echo "FAIL  empty: zero count not reported"; echo "$out"; fail=1
else
  echo "ok    empty: exit 0, '0 references checked'"
fi

exit $fail
