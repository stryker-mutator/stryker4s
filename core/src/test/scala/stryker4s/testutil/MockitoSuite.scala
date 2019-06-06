package stryker4s.testutil
import org.mockito.scalatest.MockitoSugar

trait MockitoSuite extends MockitoSugar {
  // Will cause a compile error if MockitoSuite is used without Stryker4sSuite
  this: Stryker4sSuite =>
}
