package stryker4s

package object report {
  type Reporter = FinishedRunReporter with ProgressReporter
}
