package stryker4s.testutil

import org.mockito.scalatest.MockitoSugar

trait MockitoSuite extends MockitoSugar {
  // Will cause a compile error if MockitoSuite is used outside of a ScalaTest Suite
  this: Stryker4sSuite =>
}

// AsyncMockitoSugar doesn't seem to work well with IO-based async tests
trait MockitoIOSuite extends org.mockito.MockitoSugar with org.mockito.ArgumentMatchersSugar {
  this: Stryker4sIOSuite =>
}
