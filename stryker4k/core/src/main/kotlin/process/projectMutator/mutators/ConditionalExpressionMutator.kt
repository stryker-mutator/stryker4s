package process.projectMutator.mutators

import models.Mutable
import models.Mutation
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtContainerNode
import utility.PsiUtility

object ConditionalExpressionMutator : Mutator<KtBinaryExpression>() {
    override val name = "ConditionalExpression"
    override val type: Class<KtBinaryExpression> =
        KtBinaryExpression::class.java
    override val finderCondition: (KtBinaryExpression) -> Boolean = { element ->
        PsiTreeUtil.getParentOfType(element, KtAnnotationEntry::class.java) == null &&
        element.parent::class == KtContainerNode::class && element.operationReference.text in listOf(
            "<", "<=", ">", ">=", "==", "!=", "===", "!==", "||", "&&"
        ) &&
        !element.left!!.text.contains("null") &&
        !element.right!!.text.contains("null")
    }

    override fun mutateElement(mutable: Mutable): MutableList<Mutation> {
        val mutationTextList = arrayOf("true", "false")

        return mutationTextList.map {
            Mutation(mutable, PsiUtility.createPsiElement(it), name)
        } as MutableList<Mutation>
    }
}
