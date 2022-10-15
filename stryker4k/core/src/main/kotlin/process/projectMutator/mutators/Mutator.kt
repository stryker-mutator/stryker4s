package process.projectMutator.mutators

import models.Mutable
import models.Mutation
import models.SourceFile
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtValueArgumentList
import utility.PsiUtility

abstract class Mutator<T> where T: KtElement {
    abstract val name: String
    abstract val type: Class<T>
    abstract val finderCondition: (T) -> Boolean

    abstract fun mutateElement(mutable: Mutable): MutableList<Mutation>

    fun mutateFile(sourceFile: SourceFile): List<Mutable> {
        val elementsOfType = PsiUtility.findElementsInFile(sourceFile.psiFile, type)
        val filteredElements = elementsOfType.filter(finderCondition)

        val newMutables = mutableListOf<Mutable>()
        filteredElements.forEach { element ->
            val mutable = sourceFile.mutables.find {
                it.originalElement == element as KtElement
            } ?: Mutable(sourceFile, element)
            if (mutable.mutations.isEmpty()) newMutables.add(mutable)
            mutable.mutations.addAll(mutateElement(mutable))
        }

        return newMutables
    }
}
