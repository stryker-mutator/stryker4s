package stryker4s.mutants

import stryker4s.scalatest.LogMatchers
import stryker4s.testutil.Stryker4sIOSuite

class RollbackTest extends Stryker4sIOSuite with LogMatchers {
  describe("Mutator") {
    // it("should remove a non-compiling mutant") {
    //   implicit val conf: Config = Config.default.copy(
    //     baseDir = FileUtil.getResource("rollbackTest"),
    //     concurrency = 1 // Concurrency 1 to make output order predictable
    //   )

    //   val testObj1Path = FileUtil.getResource("rollbackTest/TestObj1.scala")
    //   val testObj2Path = FileUtil.getResource("rollbackTest/TestObj2.scala")

    //   val testFiles = Seq(testObj1Path, testObj2Path)

    //   val testSourceCollector = new TestFileResolver(testFiles)

    //   val mutator = new Mutator(
    //     new MutantFinder(),
    //     new MutantCollector(new TraverserImpl(), new MutantMatcherImpl()),
    //     new MutantInstrumenter(InstrumenterOptions.sysContext(ActiveMutationContext.sysProps))
    //   )

    //   val errs = List(
    //     CompilerErrMsg("value forall is not a member of object java.nio.file.Files", "rollbackTest/TestObj1.scala", 7)
    //   )
    //   fail()
    // val ret = mutator.mutate(testSourceCollector.files, errs)

    // ret.asserting { files =>
    //   val testObj1Mutated = files.head
    //   val testObj2Mutated = files.last

    //   testObj1Mutated.fileOrigin shouldBe testObj1Path
    //   testObj1Mutated.tree.structure shouldBe FileUtil
    //     .getResource("rollbackTest/TestObj1MutatedWithoutForall.scala")
    //     .toNioPath
    //     .parse[Source]
    //     .get
    //     .structure

    //   testObj1Mutated.mutants.size shouldBe 4

    //   testObj1Mutated.nonCompilingMutants.size shouldBe 1
    //   testObj1Mutated.nonCompilingMutants.head.mutationType shouldBe Forall

    //   testObj1Mutated.excludedMutants shouldBe 0

    //   testObj2Mutated.fileOrigin shouldBe testObj2Path
    //   testObj2Mutated.tree.structure shouldBe FileUtil
    //     .getResource("rollbackTest/TestObj2Mutated.scala")
    //     .toNioPath
    //     .parse[Source]
    //     .get
    //     .structure
    //   testObj2Mutated.mutants.size shouldBe 2
    //   testObj2Mutated.nonCompilingMutants.size shouldBe 0
    //   testObj2Mutated.excludedMutants shouldBe 0
    // }
    // }
  }
}
