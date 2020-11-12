package stryker4s.testutil

import org.mockito.cats.MockitoCats
import org.mockito.scalatest.{AsyncMockitoSugar, MockitoSugar}

trait MockitoSuite extends MockitoSugar {
  // Will cause a compile error if MockitoSuite is used outside of a ScalaTest Suite
  this: Stryker4sSuite =>
}

// AsyncMockitoSugar doesn't seem to work well with IO-based async tests
trait MockitoIOSuite extends AsyncMockitoSugar with MockitoCats {
  this: Stryker4sIOSuite =>
}
