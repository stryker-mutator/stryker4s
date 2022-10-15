package models

import org.jetbrains.kotlin.psi.KtFile

class SourceFile(val path: String, val psiFile: KtFile, val mutables: MutableList<Mutable> = mutableListOf()) {
    lateinit var originalText: String

    fun getText(): String = psiFile.text
}
