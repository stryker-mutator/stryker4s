package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.Instrumenter
import stryker4jvm.core.model.MutantWithId
import stryker4jvm.mutator.kotlin.utility.PsiUtility

class KotlinInstrumenter : Instrumenter<KotlinAST> {

    // note that in kotlin we replace the children in the original source
    // unlike scala variant
    override fun instrument(source: KotlinAST?, mutations: MutableMap<KotlinAST, List<MutantWithId<KotlinAST>>>?, config: LanguageMutatorConfig): KotlinAST? {
        if (source == null || mutations == null)
            return null // or throw exception?

        mutations.forEach { (original, mutations) ->
            val whenExpression = whenExpressionGenerator(original, mutations)
            PsiUtility.replacePsiElement(original.tree, whenExpression)
        }

        return source
    }

    private fun whenExpressionGenerator(original: KotlinAST, mutations : List<MutantWithId<KotlinAST>>): KtElement {
        var whenExpressionString = "when(System.getenv(\"ACTIVE_MUTATION\") ?: null) {"
        mutations.forEach { mutation ->
            whenExpressionString += "\n\"${mutation.id}\" -> ${mutation.mutatedCode.mutatedStatement.tree.text}"
        }
        whenExpressionString += "\nelse -> ${original.tree.text}\n}"

        return PsiUtility.createPsiElement(whenExpressionString)
    }
}