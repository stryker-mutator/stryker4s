#!/usr/bin/env bash
# Asserts that the generated mutation report contains mutants produced by the custom mutator
# registered via the ServiceLoader provider, and that every mutant in the fixture was killed.
set -euo pipefail

# Reports are written to a timestamped directory, so pick the newest rather than an arbitrary one.
report=$(find target/stryker4s-report -name report.json -printf "%T@ %p\n" | sort -rn | head -n1 | cut -d" " -f2-)

if [ -z "$report" ]; then
  echo "No report.json found under target/stryker4s-report" >&2
  exit 1
fi

if ! grep -q "ArithmeticOperator" "$report"; then
  echo "report.json does not contain any ArithmeticOperator mutants: $report" >&2
  exit 1
fi

# CompileError: the mutant didn't substitute into the whole placeable statement. Survived: an untested mutant.
for status in CompileError Survived; do
  if grep -q "\"$status\"" "$report"; then
    echo "report.json contains $status mutants, expected every mutant to be Killed: $report" >&2
    exit 1
  fi
done

echo "Found ArithmeticOperator mutants, all mutants were killed, in $report"
