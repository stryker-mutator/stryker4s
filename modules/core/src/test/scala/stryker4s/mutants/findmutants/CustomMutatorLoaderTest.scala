package stryker4s.mutants.findmutants

import stryker4s.exception.{
  CustomMutatorClassNotFoundException,
  CustomMutatorInstantiationException,
  CustomMutatorNotAssignableException
}
import stryker4s.mutatorapi.CustomMutator
import stryker4s.testkit.Stryker4sSuite

class CustomMutatorLoaderTest extends Stryker4sSuite {
  private def classLoader = getClass.getClassLoader

  test("load should instantiate a valid CustomMutator by fully-qualified class name") {
    val result =
      CustomMutatorLoader.load(Seq(classOf[CustomMutatorLoaderTestFixture].getName), classLoader)

    assertEquals(result.length, 1)
    assert(result.head.isInstanceOf[CustomMutatorLoaderTestFixture])
  }

  test("load should return an empty list when given no class names") {
    assertEquals(CustomMutatorLoader.load(Seq.empty, classLoader), Nil)
  }

  test("load should instantiate multiple CustomMutators in order") {
    val result = CustomMutatorLoader.load(
      Seq(classOf[CustomMutatorLoaderTestFixture].getName, classOf[CustomMutatorLoaderTestFixture].getName),
      classLoader
    )

    assertEquals(result.length, 2)
  }

  test("load should throw CustomMutatorClassNotFoundException for an unknown class name") {
    intercept[CustomMutatorClassNotFoundException] {
      CustomMutatorLoader.load(Seq("com.example.DoesNotExist"), classLoader)
    }
  }

  test("load should throw CustomMutatorNotAssignableException for a class not extending CustomMutator") {
    intercept[CustomMutatorNotAssignableException] {
      CustomMutatorLoader.load(Seq(classOf[NotACustomMutatorFixture].getName), classLoader)
    }
  }

  test("load should throw CustomMutatorInstantiationException for a class without a no-arg constructor") {
    intercept[CustomMutatorInstantiationException] {
      CustomMutatorLoader.load(Seq(classOf[NoNoArgConstructorFixture].getName), classLoader)
    }
  }
}

/** Fixture: a valid, no-arg-constructible [[CustomMutator]]. */
class CustomMutatorLoaderTestFixture extends CustomMutator {
  def matcher: stryker4s.mutatorapi.MutationMatcher = PartialFunction.empty
}

/** Fixture: a class that does not extend [[CustomMutator]]. */
class NotACustomMutatorFixture

/** Fixture: extends [[CustomMutator]] but has no no-argument constructor. */
class NoNoArgConstructorFixture(unused: String) extends CustomMutator {
  def matcher: stryker4s.mutatorapi.MutationMatcher = {
    val _ = unused
    PartialFunction.empty
  }
}
