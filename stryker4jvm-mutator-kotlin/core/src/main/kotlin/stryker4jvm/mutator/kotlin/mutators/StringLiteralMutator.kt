package stryker4jvm.mutator.kotlin.mutators

import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import stryker4jvm.mutator.kotlin.utility.PsiUtility

object StringLiteralMutator : Mutator<KtStringTemplateExpression>() {
    override val name = "StringLiteral"
    override val type: Class<KtStringTemplateExpression> =
        KtStringTemplateExpression::class.java
    override val finderCondition: (KtStringTemplateExpression) -> Boolean = { element ->
        PsiTreeUtil.getParentOfType(element, KtAnnotationEntry::class.java) == null &&
        PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java) == null
    }

    override fun mutateElement(original: KtElement): List<KtElement> {
        val code = if (original.text == "\"\"") "\"Stryker was here!\"" else "\"\""
        return listOf(PsiUtility.createPsiElement(code))
    }
}
