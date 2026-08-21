#!/usr/bin/env bash
# Asserts that the generated mutation report contains at least one mutant produced by each of the
# three custom mutators registered via `strykerCustomMutators` in build.sbt.
set -euo pipefail

report=$(find target/stryker4s-report -name report.json | head -n1)

if [ -z "$report" ]; then
  echo "No report.json found under target/stryker4s-report" >&2
  exit 1
fi

for mutator in ArithmeticOperator NumericLiteral CollectionLiteral; do
  if ! grep -q "$mutator" "$report"; then
    echo "report.json does not contain any $mutator mutants: $report" >&2
    exit 1
  fi
done

echo "Found ArithmeticOperator, NumericLiteral, and CollectionLiteral mutant(s) in $report"
