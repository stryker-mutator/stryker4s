# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Stryker4s is a mutation testing framework for Scala. It mutates source files, recompiles once, and runs the test suite per mutant to see which mutations survive. It ships as four build-tool integrations (sbt, Mill, Maven, standalone command runner) over a shared `core`.

## Commands

The build is **sbt 2.0.x**. Prefer `sbt --client` to reuse the running server. In VS Code, prefer the Metals MCP tools (`compile-module`, `test`, `format-file`) over shelling out.

```bash
# Compile + unit tests, matching CI
sbt 'compile; ...@scalaBinaryVersion=3/test'

# Single suite, then narrowing within it (munit inherits junit-interface's filter args)
sbt 'core/testOnly stryker4s.mutants.MutatorTest'
sbt 'core/testOnly stryker4s.mutants.MutatorTest -- *some glob*'
sbt 'core/testOnly stryker4s.mutants.MutatorTest -- "--tests=.*regex.*"'

# Formatting (scalafmt is run via the wrapper script, not an sbt plugin)
./bin/scalafmt
./bin/scalafmt --test    # CI check

# Integration tests (each publishes the artifacts it needs first)
sbt 'sbtPlugin/scripted; sbtPlugin3/scripted'   # modules/sbt/src/sbt-test/
sbt 'millPlugin/millScripted'                    # modules/mill/src/mill-test/
sbt publishM2Local && cd maven && mvn verify     # Maven plugin

# Dogfooding: run Stryker4s on itself
sbt 'core/stryker'          # also commandRunner/stryker, millPlugin/stryker
```

Local-publish aliases (defined in `build.sbt`) for testing a plugin against a real project: `publishPluginLocal`, `publishMillLocal`, `publishCommandRunnerLocal` (all `0.0.0-TEST-SNAPSHOT`), `publishM2Local` (`SET-BY-SBT-SNAPSHOT`, for Maven).

Commits follow Conventional Commits — release-please derives releases from them.

## Module graph

```
api ──────────────┐              (Logger/Level only, no deps)
testRunnerApi ────┼── core ── commandRunner
   (protobuf)     │     ├───── sbtPlugin / sbtPlugin3
testRunner ───────┘     ├───── millPlugin
                        └───── maven/ (separate Maven build)
testkit ── test-only munit base suites, used by core/sbt/mill/commandRunner
```

- **`core`** — everything build-tool agnostic: config, file resolution, mutation, run orchestration, reporting.
- **`api`**, **`testRunnerApi`**, **`testRunner`** — cross-built to Scala 2.12/2.13/3.3-LTS because they are injected onto the *user's* classpath. Keep them dependency-light and LTS-compatible.
- **`maven/`** is not part of the sbt build. It is a Maven project that consumes `stryker4s-core` from `~/.m2`; run `sbt publishM2Local` before touching it.

### Scala versions (`project/Dependencies.scala`)

`core`/`testkit` cross-build to Scala 3 and **2.12** (needed by the sbt 1.x plugin). Consequence: **core source must compile under Scala 2.12 with `-Xsource:3`**, not real Scala 3. Use `implicit`, not `given`; wildcard `*` imports and `?` are fine. Version-specific code goes in `src/main/scala-2.13-` / `scala-2.13+`. The unqualified sbt project id (e.g. `core`) is the Scala 3 variant; `core2_12` is the 2.12 one.

The sbt plugin has two variants: `sbtPlugin` (Scala 2.12 / sbt 1.x) and `sbtPlugin3` (Scala 3 / sbt 2.x), with a `PluginCompat` shim per version under `modules/sbt/src/main/scala-2.12` and `scala-3`. The Mill plugin is pinned to the Scala version of the minimum supported Mill.

## Architecture

### Mutation switching

The central technique (see [docs/contributing.md](docs/contributing.md)). Rather than compiling once per mutant, all mutants for a statement are compiled into a single pattern match and selected at runtime:

```scala
_root_.stryker4s.activeMutation match {
  case 1 => person.age > 18
  case _ =>
    _root_.stryker4s.coverage.coverMutant(1)  // coverage collection
    person.age >= 18                           // original
}
```

This means generated code must always compile. Two consequences run through the codebase: the sbt runner strips fatal-warning/unused scalac options from the target project, and mutants that *do* cause compile errors are located and rolled back (`RollbackHandler`, `MutantInstrumenter.mutantIdsForCompileErrors`) before a retry run.

### Pipeline

`Stryker4sRunner` (abstract, one impl per build tool) wires the graph and runs `Stryker4s.run()`:

1. **`FileResolver`** (`GlobFileResolver`) → stream of files matching the `mutate` globs.
2. **`Mutator`** ([modules/core/.../mutants/Mutator.scala](modules/core/src/main/scala/stryker4s/mutants/Mutator.scala)) — an fs2 pipeline: parse with scalameta (`MutantFinder`) → `MutantCollector` walks the tree (`TreeTraverser`) and `MutantMatcher` produces candidate mutations → assign ids → split ignored vs. active → `MutantInstrumenter` rewrites each file into mutation switches. Parallelism is `Config.cpuParallelism`.
3. **`MutantRunner`** — copies the project into a temp dir under `target/` with mutated files substituted, builds a `TestRunnerPool`, does an **initial test run** (must pass; also collects coverage + a timeout baseline), then runs each covered mutant. Mutants that are static or uncovered are short-circuited to `Ignored`/`NoCoverage` without running tests.
4. **`Reporter`** — `AggregateReporter` fans out to console/HTML/JSON/dashboard; the report is `mutation-testing-elements` JSON written to `target/stryker4s-report/<timestamp>`.

`TestRunner` in [run/TestRunner.scala](modules/core/src/main/scala/stryker4s/run/TestRunner.scala) is a stack of decorators over a base runner: `timeoutRunner`, `retryRunner`, `maxReuseTestRunner`. Each wraps a `Resource` and can release-and-recreate the underlying process on timeout or crash (`ResourceExtensions.selfRecreatingResource`).

### Test runner protocol

For the sbt/Mill/Maven runners, tests execute in a **forked JVM** running `testRunner`'s `TestRunnerMain`, talking protobuf over a socket. The schema is [api.proto](modules/testRunnerApi/src/main/protobuf/stryker4s/testrunner/api.proto) (ScalaPB, `preserve_unknown_fields: false` deliberately). Test discovery goes through the `sbt.testing` interface, mapped by `TestInterfaceMapper`. Changing the proto is a compatibility concern across all three plugins — the test runner jar may be an older published version than core.

`legacyTestRunner` is the fallback in-sbt-session runner: slower, no compiler-error detection, and it activates mutants via system properties instead of the socket protocol (`InstrumenterOptions.sysContext` vs `InstrumenterOptions.testRunner`).

### Configuration

Config is layered via ciris. Each source implements `ConfigSource` (returning `ConfigValue` per key, or `notSupported`) and declares a `ConfigOrder` priority — **lower value wins**. `ConfigSource.aggregate` merges CLI args, the build tool's own settings (`SbtConfigSource`, `MillConfigSource`, `MavenConfigSource`), `stryker4s.conf` (HOCON), and `DefaultsConfigSource`. `ConfigLoader` `parMapN`s them into the single `Config` case class, which is then threaded as an `implicit` through nearly every class.

Adding a config option means touching: `Config`, the `ConfigSource` trait, `DefaultsConfigSource`, `CliConfigSource`, `FileConfigSource`, and each build tool's source.

## Functional style

This is a Typelevel-stack codebase (cats, cats-effect 3, fs2, ciris, munit-cats-effect). Write code that matches it; do not introduce imperative or `Future`-based alternatives.

**Effects.** Everything effectful returns `IO` (or `F[_]` — see below); nothing runs at construction time. Side-effecting library calls are wrapped: `IO(...)` for cheap synchronous work such as logging, `IO.blocking(...)` for filesystem, socket, `Desktop`, and protobuf I/O so it lands on the blocking pool. Never call `unsafeRunSync()` outside a plugin entry point — the only legitimate sites are the build-tool `main`s (`Stryker4sPlugin`, `Stryker4sModule`, maven's `Stryker4sMain`) and `Config.default`, which is test-only and documented as such.

**Tagless final at the edges, concrete `IO` in the middle.** The config layer is abstract (`ConfigSource[F]`, `ConfigLoader.load[F: Async]`, `DashboardConfigProvider[F]`, `Providers` with `F: Monad: Env`) because ciris and the tests instantiate it at different types. The run pipeline — `Stryker4s`, `Mutator`, `MutantRunner`, `TestRunner`, `Reporter` — is concrete `IO`. Don't abstract the pipeline over `F[_]`, and don't hardcode `IO` into a new `ConfigSource`. Ask for the weakest constraint that works (`Functor`/`Monad`/`Sync`/`Async`), as `Providers` does.

**Resources and concurrency.** Anything with a lifecycle is a `Resource`, never manual acquire/release — forked test-runner JVMs, socket connections, the temp directory (`Resource.makeCase`, so the finalizer can distinguish success from error and leave the dir behind for debugging). Concurrency comes from fs2 combinators and cats-effect primitives (`Ref`, `Deferred`, `parEvalMapUnordered`, `parTraverse`), not from threads or locks. Note the two distinct parallelism knobs: `Config.cpuParallelism` (all cores, for parsing/mutating/writing) versus `config.concurrency` (sized for heavyweight test-runner processes).

**Streaming.** Large or unbounded collections stay in `fs2.Stream` end to end — file discovery, mutation, and mutant execution never materialize a full list. Reach for `Pipe[IO, A, B]` for reusable stages (as `Mutator.updateWithId` and `MutantRunner.writeMutatedFile` do).

**Errors.** Expected, recoverable failures are values: `Either`, `EitherT`, `Ior`, `Option`, or a ciris `ConfigValue` failure. Compiler errors are the model case — they flow as `Either[NonEmptyList[CompilerErrMsg], _]` through `MutantRunner.handleRollback` and only become an exception (`UnableToFixCompilerErrorsException`) once genuinely unrecoverable. Fatal errors are `IO.raiseError` with a case class from the sealed `Stryker4sException` hierarchy in [exception/stryker4sException.scala](modules/core/src/main/scala/stryker4s/exception/stryker4sException.scala); add a new case there rather than throwing a bare `RuntimeException`, and mix in `NoStackTrace` when the message alone is the user-facing output. Raw `throw` is tolerated only inside scalameta `transform`/`PartialFunction` callbacks that cannot return an effect (`MutantInstrumenter`, `MutantMatcher`) — the caller catches it via `Either.catchNonFatal`.

**Types.** Prefer types that make illegal states unrepresentable: `NonEmptyList`/`NonEmptyVector`/`NonEmptySet` when emptiness is a bug (used ~50 times in core), `Ior` when both halves can be present at once, newtypes/`AnyVal` wrappers such as `MutantId`, `TestRunnerId`, `ConfigOrder` instead of bare `Int`. Sealed traits + ADTs for closed sets (`ReporterType`, `ScoreStatus`, `IgnoredMutationReason`).

**Typeclasses over ad-hoc helpers.** Define an instance rather than a utility method: `Eq[Tree]` (`treeEq`), `Order[ConfigOrder]`, `Order[SourceReplacement]`, `Show[CompilerErrMsg]`, `Monoid[PartialFunction[A, B]]`. Then use the cats syntax that comes with it — `===`/`=!=` rather than `==` on trees, `show`/`mkString_` rather than `toString`, `foldMap`/`combineAll` rather than manual folds, `.some`/`.asRight`/`.void`/`.as` rather than the stdlib constructors.

**Imports.** `import cats.syntax.all.*` is the default (26 uses in core); narrower imports like `cats.syntax.option.*` are fine when that is all you need. Keep imports organized — scalafix `OrganizeImports` is the only configured rule.

Remember the Scala 2.12 cross-build constraint above: `implicit`, not `given`; no Scala 3-only syntax in `core`.

## Conventions

- **Tests**: munit via `testkit` — extend `Stryker4sSuite` (sync) or `Stryker4sIOSuite` (cats-effect, from munit-cats-effect — return `IO[Unit]` from the test body instead of running it). `Stryker4sAssertions` provides `loneElement`, `assertSameElements`, `.value`/`.leftValue`, structural tree comparison, and `"...".parseTerm`/`parseStat`/`parseSource` helpers. `LogMatchers` + `TestLogger` for asserting on log output. Suites are named `<Thing>Test`.
- Formatting is enforced in CI. Run `./bin/scalafmt` (not `sbt scalafmtAll`) — the dialect varies per path (`fileOverride` in [.scalafmt.conf](.scalafmt.conf)); `core`/`sbt` are `scala213source3`, `mill`/`commandRunner`/`maven`/`project` are `Scala3`.
- Scalafix is configured with `OrganizeImports` only.
- New mutators live in `modules/core/src/main/scala/stryker4s/mutation/` and are registered in `MutantMatcherImpl.allMatchers`.
