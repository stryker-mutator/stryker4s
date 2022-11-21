package stryker4jvm.mutator.kotlin.mutators

import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.mutator.kotlin.utility.PsiUtility

abstract class Mutator<T> where T : KtElement {
    abstract val name: String
    abstract val type: Class<T>
    abstract val finderCondition: (T) -> Boolean

    /**
     * Function that should return a list which provides the possible mutations of the provided element.
     */
    abstract fun mutateElement(original: KtElement): List<KtElement>

    /**
     * Returns a map of which the key is the original KtElement and the value is the list of possible mutations
     */
    fun mutateFile(tree: KtElement): Map<KtElement, List<KtElement>> {
        val elementsOfType = PsiUtility.findElementsInFile(tree, type)
        val filteredElements = elementsOfType.filter(finderCondition)

        val result = mutableMapOf<KtElement, MutableList<KtElement>>()

        filteredElements.forEach { element ->
            val mutations = result.getOrDefault(element, mutableListOf())
            mutations.addAll(mutateElement(element))
            result[element] = mutations
        }

        return result
    }
}
