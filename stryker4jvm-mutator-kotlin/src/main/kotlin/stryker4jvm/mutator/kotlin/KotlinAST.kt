package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.core.model.AST

class KotlinAST(val tree: KtElement) : AST() {

    override fun syntax(): String {
        return tree.text
    }
}