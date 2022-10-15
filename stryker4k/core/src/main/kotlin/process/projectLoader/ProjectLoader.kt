package process.projectLoader

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import models.ConfigLoader
import models.Configuration
import models.SourceFile
import process.testRunner.ConsoleReporter
import process.testRunner.HTMLReporter
import utility.FileUtility
import utility.LoggingUtility
import utility.PsiUtility
import java.io.IOException
import kotlin.system.exitProcess

object ProjectLoader {
    val logger = LoggingUtility()

    fun loadProject(path: String): MutableList<SourceFile> {
        loadConfig(path)

        return loadKotlinFiles(path)
    }

    private fun loadConfig(path: String) {
        Configuration.sourcePath = path.replace("\\", "/")
        try {
            val confFileContent = FileUtility.readFile("$path/stryker-conf.json")

            val gson = Gson()
            val config = gson.fromJson(confFileContent, ConfigLoader::class.java)

            if (config.command != null)
                Configuration.command = config.command

            if (config.reporters != null)
                Configuration.reporters = config.reporters.map {
                    when (it) {
                        "console" -> ConsoleReporter()
                        "html" -> HTMLReporter()
                        else -> throw IOException("Given reporter option: '$it' is not supported")
                    }
                }
        } catch (e: IOException) {
            logger.info { "Using default configuration" }
            println(e)
        } catch (e: JsonSyntaxException) {
            logger.info { "Failed to load configuration file. Using default configuration" }
            println(e)
        }
    }

    private fun loadKotlinFiles(path: String): MutableList<SourceFile> {
        val kotlinFiles = mutableListOf<SourceFile>()
        try {
            FileUtility.readDir(path).forEach {
                if (!it.contains(Regex("""src[\\/]test""")) && it.endsWith(".kt")) {
                    val psiFile = PsiUtility.createPsiFile(FileUtility.readFile(it))
                    kotlinFiles.add(SourceFile(it.substring(path.length), psiFile))
                }
            }
        } catch (e: IOException) {
            logger.info { "Unable to read project source files" }
            println(e)
            exitProcess(1)
        }

        return kotlinFiles
    }
}
