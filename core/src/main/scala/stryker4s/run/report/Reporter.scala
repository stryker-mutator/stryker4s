package stryker4s.run.report

import stryker4s.model.MutantRunResults

class Reporter {

  private val report: MutantRunResults => Unit = ???

  val r = report andAlso (s => println(s))

  /**
    * Generate a report for each reporter that is available.
    */
  def report(runResult: MutantRunResults): Unit = {
//    config.reporters.foreach { _.reportFinishedRun(runResult) }
  }

  implicit class FuncExtensions[-T, +U](func: T => U) {

    def andAlso[A <: T, B >: U](alsoFunc: A => B): A => B = (a: A) => {
      func(a)
      alsoFunc(a)
    }
  }
}
