import process.projectMutator.MutationGenerator
import process.projectMutator.MutationPlacer
import process.projectMutator.ProjectCopyMaker
import process.projectLoader.ProjectLoader
import process.testRunner.CommandTestRunner
import utility.FileUtility
import utility.LoggingUtility
import java.io.IOException
import kotlin.system.exitProcess

object Stryker4k {
    val logger = LoggingUtility()

    fun run(args: Array<String> = arrayOf()) {
        val projectToTestPath = if (args.isNotEmpty()) {
            args[0]
        } else {
            try {
                FileUtility.getAbsolutePath("")
            } catch (e: IOException) {
                logger.info { "Failed to find project path." }
                println(e)
                exitProcess(1)
            }
        }

        logger.info { "Loading project" }
        val sourceFiles = ProjectLoader.loadProject(projectToTestPath)
        logger.info { "Generating mutations" }
        val mutations = MutationGenerator.generateMutations(sourceFiles)
        logger.info { "Making project copy" }
        val tempDirPath = ProjectCopyMaker.copySourceProject(projectToTestPath)
        logger.info { "Placing mutations in copy" }
        MutationPlacer.placeMutations(sourceFiles, tempDirPath)
        logger.info { "Preparing to run mutants" }
        CommandTestRunner.run(mutations, tempDirPath)

        try {
            FileUtility.deleteTempDir(tempDirPath)
        } catch (e: IOException) {
            logger.info { "Failed to delete project copy." }
            println(e)
        }
    }
}
