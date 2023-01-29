package stryker4jvm.mutator.kotlin.mutators

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.mutator.kotlin.utility.PsiUtility

object BooleanLiteralMutator : Mutator<KtConstantExpression>() {
  override val name = "BooleanLiteral"
  override val type: Class<KtConstantExpression> = KtConstantExpression::class.java
  override val finderCondition: (KtConstantExpression) -> Boolean = { element ->
    PsiTreeUtil.getParentOfType(element, KtAnnotationEntry::class.java) == null &&
        element.node.elementType == KtNodeTypes.BOOLEAN_CONSTANT
  }

  override fun mutateElement(original: KtElement): List<KtElement> {
    val code = if (original.text == "true") "false" else "true"
    val mutatedStatement = PsiUtility.createPsiElement(code)
    return mutableListOf(mutatedStatement)
  }
}
