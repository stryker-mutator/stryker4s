#!/usr/bin/env bash
# Asserts that the generated mutation report contains at least one mutant produced by each of the
# custom mutators registered via `strykerCustomMutators` in build.sbt, and that every mutant in the
# fixture was killed.
set -euo pipefail

# Reports are written to a timestamped directory, so pick the newest rather than an arbitrary one.
report=$(find target/stryker4s-report -name report.json -printf "%T@ %p\n" | sort -rn | head -n1 | cut -d" " -f2-)

if [ -z "$report" ]; then
  echo "No report.json found under target/stryker4s-report" >&2
  exit 1
fi

for mutator in ArithmeticOperator NumericLiteral CollectionLiteral ErrorHandling ConditionalEffect FallbackRemoval FallbackInversion SequencingSwap SequencingRemoval TimeoutRemoval GetOrElse ParallelSequentialSwap ResourceFinalizer RefUpdate SleepRemoval; do
  if ! grep -q "$mutator" "$report"; then
    echo "report.json does not contain any $mutator mutants: $report" >&2
    exit 1
  fi
done

# A CompileError means a mutator produced a replacement that does not type-check in context — most
# often by returning the bare replacement instead of the whole placeable statement (see
# `PlaceableTree.substitute`), or by matching both an applied call and its inner selection so that
# the second mutant leaves the argument list dangling. A Survived mutant in this fixture means an
# example mutator is no longer covered by a test, or that an equivalent mutant crept in.
for status in CompileError Survived; do
  if grep -q "\"$status\"" "$report"; then
    echo "report.json contains $status mutants, expected every mutant to be Killed: $report" >&2
    exit 1
  fi
done

echo "Found a mutant for every registered custom mutator, and all mutants were killed, in $report"
