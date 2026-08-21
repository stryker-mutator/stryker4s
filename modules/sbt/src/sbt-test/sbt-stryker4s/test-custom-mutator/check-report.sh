#!/usr/bin/env bash
# Asserts that the generated mutation report contains at least one mutant produced by the
# custom ArithmeticOperatorMutator registered via `strykerCustomMutators` in build.sbt.
set -euo pipefail

report=$(find target/stryker4s-report -name report.json | head -n1)

if [ -z "$report" ]; then
  echo "No report.json found under target/stryker4s-report" >&2
  exit 1
fi

if ! grep -q "ArithmeticOperator" "$report"; then
  echo "report.json does not contain any ArithmeticOperator mutants: $report" >&2
  exit 1
fi

echo "Found ArithmeticOperator mutant(s) in $report"
