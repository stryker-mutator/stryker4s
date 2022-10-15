package process.projectMutator

import models.SourceFile
import models.Mutable
import org.jetbrains.kotlin.psi.KtElement
import utility.FileUtility
import utility.LoggingUtility
import utility.PsiUtility
import java.io.IOException

object MutationPlacer {
    val logger = LoggingUtility()

    var currentMutationNumber = 1

    fun placeMutations(sourceFiles: List<SourceFile>, tempDirPath: String) {
        sourceFiles.forEach { sourceFile ->
            sourceFile.originalText = sourceFile.getText()
            sourceFile.mutables.forEach { mutable ->
                val whenExpression = whenExpressionGenerator(mutable)
                PsiUtility.replacePsiElement(mutable.originalElement, whenExpression)
            }

            try {
                FileUtility.writeFile(tempDirPath + sourceFile.path, sourceFile.getText())
            } catch (e: IOException) {
                logger.info { "Failed to mutate ${sourceFile.path}" }
                println(e)
            }
        }
    }

    private fun whenExpressionGenerator(mutable: Mutable): KtElement {
        var whenExpressionString = "when(System.getenv(\"ACTIVE_MUTATION\") ?: null) {"
        mutable.mutations.forEach { mutation ->
            mutation.id = currentMutationNumber
            whenExpressionString += "\n\"${mutation.id}\" -> ${mutation.getText()}"
            currentMutationNumber++
        }
        whenExpressionString += "\nelse -> ${mutable.getText()}\n}"

        return PsiUtility.createPsiElement(whenExpressionString)
    }
}
