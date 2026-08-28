package example

import scala.meta.*

/** Unit tests for [[ResourceFinalizerMutator]], written first (TDD) before the mutator implementation.
  */
class ResourceFinalizerMutatorTest extends munit.FunSuite {
  private val mutator = new ResourceFinalizerMutator

  private def parseTerm(code: String): Term =
    code.parse[Term].get

  private def mutate(code: String) = {
    val term = parseTerm(code)
    assert(mutator.matcher.isDefinedAt(term), s"expected matcher to be defined at: $code")
    val Right(mutations) =
      mutator.matcher(term)(stryker4s.mutatorapi.PlaceableTree(term)): @unchecked
    mutations
  }

  test("ResourceFinalizerMutator drops the release of Resource.make") {
    val mutations = mutate("Resource.make(openFile)(closeFile)")

    assertEquals(mutations.length, 1)
    assertEquals(mutations.head.metadata.mutatorName, "ResourceFinalizer")
    assertEquals(mutations.head.mutatedStatement.syntax, "Resource.eval(openFile)")
  }

  test("ResourceFinalizerMutator drops the release of Resource.makeCase") {
    val mutations = mutate("Resource.makeCase(openFile)((f, _) => closeFile(f))")

    assertEquals(mutations.head.mutatedStatement.syntax, "Resource.eval(openFile)")
  }

  test("ResourceFinalizerMutator handles a qualified Resource reference") {
    val mutations = mutate("cats.effect.Resource.make(openFile)(closeFile)")

    assertEquals(mutations.head.mutatedStatement.syntax, "cats.effect.Resource.eval(openFile)")
  }

  test("ResourceFinalizerMutator turns bracket into a plain flatMap") {
    val mutations = mutate("openFile.bracket(useFile)(closeFile)")

    assertEquals(mutations.head.mutatedStatement.syntax, "openFile.flatMap(useFile)")
  }

  test("ResourceFinalizerMutator turns bracketCase into a plain flatMap") {
    val mutations = mutate("openFile.bracketCase(useFile)((f, _) => closeFile(f))")

    assertEquals(mutations.head.mutatedStatement.syntax, "openFile.flatMap(useFile)")
  }

  test("ResourceFinalizerMutator removes guarantee, leaving the receiver") {
    assertEquals(mutate("work.guarantee(cleanup)").head.mutatedStatement.syntax, "work")
    assertEquals(mutate("work.guaranteeCase(cleanup)").head.mutatedStatement.syntax, "work")
    assertEquals(mutate("work.onCancel(cleanup)").head.mutatedStatement.syntax, "work")
  }

  test("ResourceFinalizerMutator records the dropped finalizer in its metadata") {
    assertEquals(mutate("work.guarantee(cleanup)").head.metadata.original, "guarantee")
    assertEquals(mutate("Resource.make(openFile)(closeFile)").head.metadata.original, "make")
  }

  test("ResourceFinalizerMutator does not match a make on an unrelated companion") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("Widget.make(a)(b)")))
  }

  test("ResourceFinalizerMutator does not match Resource.eval or Resource.pure") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("Resource.eval(openFile)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("Resource.pure(value)")))
  }

  test("ResourceFinalizerMutator does not match unrelated combinators") {
    assert(!mutator.matcher.isDefinedAt(parseTerm("work.flatMap(next)")))
    assert(!mutator.matcher.isDefinedAt(parseTerm("work.guaranteed")))
  }
}
