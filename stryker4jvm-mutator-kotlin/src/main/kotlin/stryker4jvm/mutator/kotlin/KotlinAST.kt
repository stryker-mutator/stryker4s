package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement

class KotlinAST(val tree: KtElement) : AST() {

    override fun syntax(): String {
        return tree.text
    }
}