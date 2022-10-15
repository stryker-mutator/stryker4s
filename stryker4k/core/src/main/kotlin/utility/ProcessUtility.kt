package utility

import java.io.File
import java.util.concurrent.TimeUnit

object ProcessUtility {
    private const val MAX_TEST_DURATION: Long = 10

    fun runCommand(command: String, path: String, envKey: String, envValue: String): Int {
        val commandList = mutableListOf<String>()
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            commandList.addAll(arrayOf("cmd", "/c"))
        }
        commandList.addAll(command.split(' '))

        val processBuilder = ProcessBuilder(commandList)
        processBuilder.directory(File(path))
        processBuilder.environment()[envKey] = envValue
        processBuilder.inheritIO()

        val process = startProcess(processBuilder)

        val isTerminated = process.waitFor(MAX_TEST_DURATION, TimeUnit.MINUTES)
        if (!isTerminated)
            process.destroyForcibly();

        return process.exitValue()
    }

    fun startProcess(processBuilder: ProcessBuilder): Process = processBuilder.start()
}
