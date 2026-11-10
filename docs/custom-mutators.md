---
title: Custom mutators
custom_edit_url: https://github.com/stryker-mutator/stryker4s/edit/master/docs/custom-mutators.md
sidebar_position: 4
---

Third-party mutators are discovered automatically through Java's `ServiceLoader`. Add the
`stryker4s-plugin-api` artifact to the project that provides the mutators, implement
`stryker4s.plugin.CustomMutatorPlugin`, and register the provider in
`META-INF/services/stryker4s.plugin.CustomMutatorPlugin`.

Compile the mutator-provider project for the Scala version of the Stryker4s runtime,
as described in [Scala binary compatibility](#scala-binary-compatibility) below.
The `%%` dependency uses that project's Scala binary version, not the version of the
application being mutation-tested.

```scala
libraryDependencies += "io.stryker-mutator" %% "stryker4s-plugin-api" % strykerVersion % Provided
```

```text
# src/main/resources/META-INF/services/stryker4s.plugin.CustomMutatorPlugin
com.example.ExampleMutatorPlugin
```

```scala
package com.example

import cats.data.NonEmptyVector
import cats.syntax.all.*
import stryker4s.plugin.CustomMutatorPlugin
import stryker4s.plugin.api.*

import scala.meta.*

class ExampleMutatorPlugin extends CustomMutatorPlugin {
  def mutators: List[CustomMutator] = List(new ArithmeticOperatorMutator)
}

class ArithmeticOperatorMutator extends CustomMutator {
  def matcher: MutationMatcher = {
    case term @ Term.ApplyInfix.After_4_6_0(_, op @ Term.Name("+"), _, _) =>
      placeableTree =>
        val mutated = term.copy(op = op.copy(value = "-"))
        val metadata = MutantMetadata("+", "-", "ArithmeticOperator", term.pos, None)
        placeableTree
          .substitute(term, mutated)
          .bimap(
            reason => NonEmptyVector.one(MutatedCode(term, metadata) -> reason),
            code => NonEmptyVector.one(MutatedCode(code, metadata))
          )
  }
}
```

## Scala binary compatibility

Custom mutators run in the JVM that runs Stryker4s, independently of the Scala version of
the application being mutation-tested. Compile the mutator-provider library for that runtime
and use the plugin API version corresponding to the Stryker4s version running it.

| Stryker4s integration | Mutator-provider Scala version |
| --- | --- |
| sbt 1.x | Scala 2.12 |
| sbt 2.x, Mill, Maven, command runner | The Scala 3 version supported by that Stryker4s integration |

The plugin API is published for Stryker4s's supported runtime versions, not Scala 2.13.
A Scala 2.13 application remains supported: build its custom mutators in a separate
project or module targeting the appropriate runtime from the table above. The application
does not need to change its Scala version.

Do not rely on cross-version dependency overrides or a restricted choice of collection APIs
to make a Scala 2.13 provider compatible with a Scala 2.12 or Scala 3 runtime. Matching the
Scala 3 binary suffix alone also does not guarantee compiler or macro compatibility; use the
Scala version supported by the integration.

## Returning the whole statement, not just the replacement

A matcher usually matches a small sub-term, but a `MutatedCode` must carry the **entire placeable
statement** with the mutation applied in place. `PlaceableTree.substitute(original, replacement)`
does that for you, and is what the built-in mutators use. It returns a `SubstitutionFailed` on the
left if `original` is not inside the statement; report it as an ignored mutation, as above.

Returning the bare replacement instead happens to work when the matched term *is* the whole
placeable statement, and silently produces mutants that do not compile otherwise. Matching the
`IO.sleep(d)` inside `IO.sleep(d).as(q).timeout(l)` and returning `IO.unit` yields the statement
`IO.unit`, where `IO.unit.as(q).timeout(l)` was intended.

## Match one node per mutation

Stryker4s visits every node in the tree, so a matcher that accepts both `expr.foo(arg)` and its
inner `expr.foo` selection emits **two** mutants for one combinator. The second drops only the
selection and leaves the argument list behind — `IO(a / b)(_ => IO.pure(0))` — which cannot
compile. Keep the applied and no-argument method names in disjoint sets.

## Dropping a selection needs a parent guard; renaming one does not

Disjoint name sets are not quite enough for a mutator that *drops* a selection, because a
no-argument method can still be applied explicitly: `xs.sorted(ordering)`,
`code.toLowerCase(locale)`. The inner `xs.sorted` selection matches, and dropping it strands the
argument list as `xs(ordering)`. Guard against being the function of an enclosing call:

```scala
private def isAppliedTo(term: Term): Boolean =
  term.parent.exists {
    case Term.Apply.After_4_6_0(fun, _) => fun eq term
    case _                              => false
  }
```

A mutator that only *renames* a selection — `.head` to `.last`, say — needs no such guard, since
`xs.last(i)` compiles exactly as `xs.head(i)` does.

A custom mutator's replacement(s) must always produce code that still compiles alongside the
built-in mutants for the same statement (mutation switching compiles every mutant for a given
line into one pattern match) — the same constraint the built-in mutators already satisfy.

## Example

A worked example with mutators for cats-effect lives in [stryker4s-for-cats](https://github.com/bjhartin/stryker4s-for-cats).
