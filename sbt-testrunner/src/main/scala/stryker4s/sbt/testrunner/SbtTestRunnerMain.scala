package stryker4s.sbt.testrunner

object SbtTestRunnerMain {
  def main(args: Array[String]): Unit = {

    val context = Context.resolveContext(args)

    val runner = new SbtTestRunner(context)
    runner.testMutations(Seq(1))

  }
}
