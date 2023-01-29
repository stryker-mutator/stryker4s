package stryker4jvm.mutator.kotlin.mutators

import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.mutator.kotlin.utility.PsiUtility

object EqualityOperatorMutator : Mutator<KtBinaryExpression>() {
  override val name = "EqualityOperator"
  override val type: Class<KtBinaryExpression> = KtBinaryExpression::class.java
  override val finderCondition: (KtBinaryExpression) -> Boolean = { element ->
    PsiTreeUtil.getParentOfType(element, KtAnnotationEntry::class.java) == null &&
        element.parent::class == KtContainerNode::class &&
        element.operationReference.text in listOf("<", "<=", ">", ">=", "==", "!=", "===", "!==") &&
        !element.left!!.text.contains("null") &&
        !element.right!!.text.contains("null")
  }

  override fun mutateElement(original: KtElement): List<KtElement> {
    val left = (original as KtBinaryExpression).left
    val operator = original.operationReference.text
    val right = original.right
    if (left == null || right == null) return mutableListOf()

    val mutationList =
        when (operator) {
          "<" -> arrayOf("<=", ">=")
          "<=" ->
              arrayOf(
                  "<",
                  ">",
              )
          ">" -> arrayOf(">=", "<=")
          ">=" ->
              arrayOf(
                  ">",
                  "<",
              )
          "==" -> arrayOf("!=")
          "!=" -> arrayOf("==")
          "===" -> arrayOf("!==")
          "!==" -> arrayOf("===")
          else -> arrayOf()
        }.map { PsiUtility.createPsiElement("${left.text} $it ${right.text}") }

    return mutationList
  }
}
