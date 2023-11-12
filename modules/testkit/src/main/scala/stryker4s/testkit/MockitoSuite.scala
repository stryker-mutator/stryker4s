package stryker4s.testkit

import munit.Suite
import org.mockito.cats.MockitoCats
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

protected[stryker4s] trait MockitoSuite extends MockitoSugar with ArgumentMatchersSugar with MockitoCats {
  this: Suite =>
}
