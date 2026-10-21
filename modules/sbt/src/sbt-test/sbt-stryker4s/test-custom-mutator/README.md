# SBT plugin custom mutator test project

This is a scripted test project for Stryker4s's custom mutator support
(`ServiceLoader`). It registers `example.ExampleMutatorPlugin`, which
provides a single example mutator, and asserts that the generated `report.json`
contains its mutants and that all of them were killed.

To run it, run `sbt scripted` in the root of the Stryker4s project.
