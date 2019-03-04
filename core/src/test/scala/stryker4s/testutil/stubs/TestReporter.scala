package stryker4s.testutil.stubs
import stryker4s.report.{FinishedRunReporter, ProgressReporter}

trait TestReporter extends ProgressReporter with FinishedRunReporter