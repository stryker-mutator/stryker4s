---
title: Custom mutators
custom_edit_url: https://github.com/stryker-mutator/stryker4s/edit/master/docs/custom-mutators.md
sidebar_position: 4
---

Third-party mutators are discovered automatically through Java's `ServiceLoader`. Add the
`stryker4s-plugin-api` artifact to the project that provides the mutators, implement
`stryker4s.plugin.CustomMutatorPlugin`, and register the provider in
`META-INF/services/stryker4s.plugin.CustomMutatorPlugin`.

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
        Right(NonEmptyVector.one(MutatedCode(placeableTree.substitute(term, mutated), metadata)))
  }
}
```

## Scala binary compatibility

Plugins are loaded into the JVM that runs Stryker4s, and Stryker4s's own Scala library takes
precedence. With the sbt 1.x plugin that library is Scala 2.12, so a plugin compiled for Scala 2.13
or 3 only links if it avoids standard-library APIs whose signatures changed in 2.13. For example,
return `List(...)` from `mutators`, not `Seq(...)` or `Vector(...)`. The sbt 2.x, Mill, Maven, and
command runner integrations use a Scala 3 standard library and are not affected.

## Returning the whole statement, not just the replacement

A matcher usually matches a small sub-term, but a `MutatedCode` must carry the **entire placeable
statement** with the mutation applied in place. `PlaceableTree.substitute(original, replacement)`
does that for you, and is what the built-in mutators use.

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
