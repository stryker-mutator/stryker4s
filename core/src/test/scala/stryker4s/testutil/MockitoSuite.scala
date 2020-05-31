package stryker4s.testutil
import org.mockito.scalatest.MockitoSugar
import org.scalatest.Suite

trait MockitoSuite extends MockitoSugar {
  // Will cause a compile error if MockitoSuite is used outside of a ScalaTest Suite
  this: Suite =>
}
