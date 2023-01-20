package stryker4jvm.mutants

import stryker4jvm.config.Config
import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.{AST, MutantWithId, MutatedCode}
import stryker4jvm.scalatest.FileUtil
import stryker4jvm.testutil.{MockAST, Stryker4jvmSuite, TestLanguageMutator}

import java.util
import scala.collection.JavaConverters.*
import scala.io.Source

class LanguageMutatorTest extends Stryker4jvmSuite {
  val path = FileUtil.getResource("mockFiles/simple.test").toNioPath

  describe("Mock Parser") {
    it("Should parse and give all lines") {
      val mutator = new TestLanguageMutator()
      val ast = mutator.parse(path)
      val bufferedSource = Source.fromFile(path.toFile)
      val lines = bufferedSource.getLines().toArray
      bufferedSource.close()
      val expected = lines.map(line => new MockAST(line, Array.empty))
      assert(expected.length == ast.children.length)
    }

    it("Should have the correct lines") {
      val mutator = new TestLanguageMutator()
      val ast = mutator.parse(path)
      val bufferedSource = Source.fromFile(path.toFile)
      val lines = bufferedSource.getLines().toArray
      bufferedSource.close()
      assert(ast.contents == "")
      for ((exp, act) <- lines.zip(ast.children))
        assert(exp.equals(act.contents) && act.children.isEmpty)
    }
  }

  describe("Mock Collector") {
    it("Should not collect any empty lines") {
      val mutator = new TestLanguageMutator()
      val ast = mutator.parse(path)
      val collected = mutator.collect(ast)
      collected.mutations.keySet().forEach(tree => assert(!tree.contents.equals("")))
    }

    it("Should collect all lines with a capital") {
      val mutator = new TestLanguageMutator()
      val ast = mutator.parse(path)
      val collected = mutator.collect(ast)
      collected.mutations.keySet().forEach(tree => assert(tree.contents.charAt(0).isUpper))
    }

    it("Should not exclude mutants without reason") {
      val mutator = new TestLanguageMutator()
      val ast = mutator.parse(path)
      val collected = mutator.collect(ast)
      assert(collected.ignoredMutations.isEmpty)
    }

    it("Should have a config that doesn't parse numbers") {
      val mutator = new TestLanguageMutator()
      val excludedMutants: util.HashSet[String] = new util.HashSet()
      excludedMutants.add("noNumber")
      val testLanguageConfig = new LanguageMutatorConfig("test", excludedMutants)
      mutator.collector.config = Config.default.copy(mutatorConfigs = Map("test" -> testLanguageConfig))
      val ast = mutator.parse(path)
      val collected = mutator.collect(ast)
      collected.mutations.keySet().forEach(tree => assert(!tree.contents.exists(_.isDigit), tree.contents))
    }

    it("Should include ignored mutations") {
      val mutator = new TestLanguageMutator()
      val excludedMutants: util.HashSet[String] = new util.HashSet()
      excludedMutants.add("noNumber")
      val testLanguageConfig = new LanguageMutatorConfig("test", excludedMutants)
      mutator.collector.config = Config.default.copy(mutatorConfigs = Map("test" -> testLanguageConfig))
      val ast = mutator.parse(path)
      val collected = mutator.collect(ast)
      assert(!collected.ignoredMutations.isEmpty)
      assert(collected.ignoredMutations.size() == 1)
      collected.ignoredMutations.forEach(ignored => {
        val tree = ignored.mutatedCode.mutatedStatement
        assert(tree.contents.exists(_.isDigit), tree.contents)
      })
    }
  }

  describe("Mock instrumenter") {
    it("Should add \"Mutated:\" to mutant") {
      val mutator = new TestLanguageMutator()
      val ast = mutator.parse(path)
      val bufferedSource = Source.fromFile(path.toFile)
      val lines = bufferedSource.getLines().toArray
      bufferedSource.close()
      val collected = mutator.collect(ast)
      val mutantsWithId = collected.mutations.asScala.map { case (tree, mutations) =>
        val mutantWithId = mutations.asScala
          .map(mutatedCode => new MutantWithId(0, mutatedCode.asInstanceOf[MutatedCode[AST]]))
          .asJava
        (tree.asInstanceOf[AST], mutantWithId)
      }.asJava
      val instrumented = mutator.instrument(ast, mutantsWithId)
      assert(ast != instrumented)
      for (i <- lines.indices) {
        val isMutant = lines(i).nonEmpty && lines(i).charAt(0).isUpper
        if (isMutant) {
          assert(instrumented.children(i).contents.startsWith("Mutated:"))
        }
      }

    }
  }
}
