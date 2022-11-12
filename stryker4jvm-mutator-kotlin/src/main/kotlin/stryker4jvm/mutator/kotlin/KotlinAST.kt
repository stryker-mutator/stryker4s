package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.mutants.language.AST

class KotlinAST(val tree: KtElement) : AST() {

    override fun syntax(): String {
        return tree.text
    }
}