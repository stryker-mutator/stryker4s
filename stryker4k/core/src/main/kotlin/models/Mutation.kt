package models

import org.jetbrains.kotlin.psi.KtElement

class Mutation(val mutable: Mutable, val element: KtElement, val mutatorName: String, var id: Int = -1) {
    fun getText(): String = element.text
}
