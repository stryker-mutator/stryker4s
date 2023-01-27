package stryker4jvm.mutator.scala
import stryker4jvm.mutator.scala.testutil.Stryker4jvmSuite

import scala.collection.JavaConverters.*
import scala.meta.quasiquotes.*
import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.InstrumenterOptions
import stryker4jvm.core.model.LanguageMutator

class ScalaMutatorProviderTest extends Stryker4jvmSuite {

  val mutProvider = new ScalaMutatorProvider();

  describe("MutatorProvider") {
    it("Should create a mutator") {
      val config = new LanguageMutatorConfig("Scala", Set().asJava);
      val logger = new ScalaLogger();
      val options = InstrumenterOptions.SysProp;

      val mutator = mutProvider.provideMutator(config, logger, options)
      testBooleanLiteral(mutator)
    }
  }

  def testBooleanLiteral(mutator: LanguageMutator[ScalaAST]): Unit = {
    val booleanLiteral = new ScalaAST(value = q"false");
    assert(!mutator.collect(booleanLiteral).mutations.isEmpty())
  }
}
