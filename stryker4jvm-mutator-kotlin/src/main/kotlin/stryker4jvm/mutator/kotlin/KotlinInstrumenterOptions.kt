package stryker4jvm.mutator.kotlin

import stryker4jvm.core.model.InstrumenterOptions

class KotlinInstrumenterOptions(options: InstrumenterOptions) {
    val whenExpression: String

    init {
        val key = InstrumenterOptions.KEY
        whenExpression = when (options) {
            InstrumenterOptions.EnvVar -> "when(System.getenv(\"$key\") ?: null)"
            InstrumenterOptions.SysProp -> "when(System.getProperty(\"$key\") ?: null)"
            InstrumenterOptions.TestRunner -> throw RuntimeException("Not implemented for test runner!")
            else -> throw RuntimeException("Not implemented for $options")
        }
    }
}