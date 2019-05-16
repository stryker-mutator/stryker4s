package stryker4s.testutil
import org.mockito.scalatest.MockitoSugar

trait MockitoSuite extends MockitoSugar {
  // Will cause a compile error if MockitoScala is used without Stryker4sSuite
  this: Stryker4sSuite =>
}
