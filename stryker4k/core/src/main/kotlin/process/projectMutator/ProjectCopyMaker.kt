package process.projectMutator

import utility.FileUtility
import utility.LoggingUtility
import java.io.IOException
import kotlin.system.exitProcess

object ProjectCopyMaker {
    private val logger = LoggingUtility()

    fun copySourceProject(path: String): String {
        val tempDir = FileUtility.createTempDir()

        try {
            FileUtility.readDir(path).forEach {
                val relativePath = it.substring(path.length)
                if (!it.endsWith(".lock") && relativePath.isNotEmpty())
                    FileUtility.copyFileTo(it, tempDir + relativePath)
            }
        } catch (e: IOException) {
            logger.info { "Failed to create a copy of the source project" }
            logger.info { e.stackTraceToString() }
            exitProcess(1)
        }

        return tempDir
    }
}
