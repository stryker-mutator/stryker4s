package process.projectMutator.mutators

import models.Mutable
import models.Mutation
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtExpression
import utility.PsiUtility

object LogicalOperatorMutator : Mutator<KtBinaryExpression>() {
    override val name = "LogicalOperator"
    override val type: Class<KtBinaryExpression> =
        KtBinaryExpression::class.java
    override val finderCondition: (KtBinaryExpression) -> Boolean = { element ->
        PsiTreeUtil.getParentOfType(element, KtAnnotationEntry::class.java) == null &&
        element.parent::class == KtContainerNode::class && element.operationReference.text in listOf(
            "||", "&&"
        ) &&
        !element.left!!.text.contains("null") &&
        !element.right!!.text.contains("null")
    }

    override fun mutateElement(mutable: Mutable): MutableList<Mutation> {
        val left = (mutable.originalElement as KtBinaryExpression).left
        val operator = mutable.originalElement.operationReference.text
        val right = mutable.originalElement.right
        if (left == null || right == null) return mutableListOf()

        val mutationList = when(operator) {
            "||" -> arrayOf("&&")
            "&&" -> arrayOf("||")
            else -> arrayOf()
        }.map { Mutation(
            mutable,
            PsiUtility.createPsiElement("${left.text} $it ${right.text}"),
            name
        ) }

        return mutationList as MutableList<Mutation>
    }
}
