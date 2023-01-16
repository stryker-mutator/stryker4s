package stryker4jvm.mutator.kotlin

import stryker4jvm.core.exception.UnsupportedInstrumenterOptionsException
import stryker4jvm.core.model.InstrumenterOptions
import kotlin.jvm.Throws

class KotlinInstrumenterOptions @Throws(UnsupportedInstrumenterOptionsException::class) constructor(options: InstrumenterOptions) {
    val whenExpression: String

    init {
        val key = InstrumenterOptions.KEY
        whenExpression = when (options) {
            InstrumenterOptions.EnvVar -> "when(System.getenv(\"$key\") ?: null)"
            InstrumenterOptions.SysProp -> "when(System.getProperty(\"$key\") ?: null)"
            InstrumenterOptions.TestRunner -> throw UnsupportedInstrumenterOptionsException(options)
            else -> throw UnsupportedInstrumenterOptionsException(options)
        }
    }
}