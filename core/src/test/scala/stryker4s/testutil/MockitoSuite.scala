package stryker4s.testutil

import org.mockito.cats.MockitoCats
import org.mockito.scalatest.{AsyncMockitoSugar, MockitoSugar}

trait MockitoSuite extends MockitoSugar {
  // Will cause a compile error if MockitoSuite is used outside of a ScalaTest Suite
  this: Stryker4sSuite =>
}

trait MockitoIOSuite extends AsyncMockitoSugar with MockitoCats {
  this: Stryker4sIOSuite =>
}
