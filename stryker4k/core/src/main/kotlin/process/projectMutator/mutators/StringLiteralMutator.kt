package process.projectMutator.mutators

import models.Mutable
import models.Mutation
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import utility.PsiUtility

object StringLiteralMutator : Mutator<KtStringTemplateExpression>() {
    override val name = "StringLiteral"
    override val type: Class<KtStringTemplateExpression> =
        KtStringTemplateExpression::class.java
    override val finderCondition: (KtStringTemplateExpression) -> Boolean = { element ->
        PsiTreeUtil.getParentOfType(element, KtAnnotationEntry::class.java) == null &&
        PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java) == null
    }

    override fun mutateElement(mutable: Mutable): MutableList<Mutation> {
        val code = if (mutable.getText() == "\"\"") "\"Stryker was here!\"" else "\"\""

        return mutableListOf(Mutation(mutable, PsiUtility.createPsiElement(code), name))
    }
}
