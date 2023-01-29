package stryker4jvm.mutator.kotlin

import java.lang.Exception
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import stryker4jvm.core.model.InstrumenterOptions

class KotlinInstrumenterOptionsTest {

  @Test
  fun shouldProduceWhenWithEnvironmentVariable() {
    val options = KotlinInstrumenterOptions(InstrumenterOptions.EnvVar)
    assertTrue(options.whenExpression.contains("getenv"))
    assertFalse(options.whenExpression.contains("getProperty"))
  }

  @Test
  fun shouldProduceWhenWithSystemProperty() {
    val options = KotlinInstrumenterOptions(InstrumenterOptions.SysProp)
    assertTrue(options.whenExpression.contains("getProperty"))
    assertFalse(options.whenExpression.contains("getenv"))
  }

  @Test
  fun shouldProduceException() {
    assertFailsWith<Exception> { KotlinInstrumenterOptions(InstrumenterOptions.TestRunner) }
  }
}
