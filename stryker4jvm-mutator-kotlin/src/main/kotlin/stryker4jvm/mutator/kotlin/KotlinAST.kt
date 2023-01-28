package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.core.model.AST

class KotlinAST(val tree: KtElement) : AST() {
  private val text: String = tree.text
  private val hash: Int = text.hashCode()

  override fun syntax(): String {
    return text
  }

  override fun hashCode(): Int {
    return hash
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) return false
    if (other !is KotlinAST) return false
    return tree == other.tree
  }
}
