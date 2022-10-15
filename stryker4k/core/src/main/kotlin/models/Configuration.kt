package models

import process.testRunner.ConsoleReporter
import process.testRunner.Reporter

data class ConfigLoader(
    val command: String?,
    val reporters: Array<String>? = arrayOf("console")
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfigLoader

        if (command != other.command) return false
        if (!reporters.contentEquals(other.reporters)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = command.hashCode()
        result = 31 * result + reporters.contentHashCode()
        return result
    }
}

object Configuration {
    var sourcePath: String = ""
    var command: String = "mvn test"
    var reporters: List<Reporter> = listOf(ConsoleReporter())
}
