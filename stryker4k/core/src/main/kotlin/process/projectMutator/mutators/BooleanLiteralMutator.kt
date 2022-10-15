package process.projectMutator.mutators

import models.Mutable
import models.Mutation
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtConstantExpression
import utility.PsiUtility

object BooleanLiteralMutator : Mutator<KtConstantExpression>() {
    override val name = "BooleanLiteral"
    override val type: Class<KtConstantExpression> =
        KtConstantExpression::class.java
    override val finderCondition: (KtConstantExpression) -> Boolean = { element ->
        PsiTreeUtil.getParentOfType(element, KtAnnotationEntry::class.java) == null &&
        element.node.elementType == KtNodeTypes.BOOLEAN_CONSTANT
    }

    override fun mutateElement(mutable: Mutable): MutableList<Mutation> {
        val code = if (mutable.getText() == "true") "false" else "true"
        return mutableListOf(Mutation(mutable, PsiUtility.createPsiElement(code), name))
    }
}
