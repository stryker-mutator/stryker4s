# SBT plugin custom mutator test project

This is a scripted test project for Stryker4s's custom mutator support
(`strykerCustomMutators`). It registers `example.ArithmeticOperatorMutator`, a small
example custom mutator that swaps `+` for `-` (and vice versa) on `Int` arithmetic,
and asserts that the generated `report.json` contains at least one killed mutant
produced by that custom mutator (`mutatorName == "ArithmeticOperator"`).

To run it, run `sbt scripted` in the root of the Stryker4s project.
