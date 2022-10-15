package models

import mutationtesting.Location
import mutationtesting.Position
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class Mutable(
    val sourceFile: SourceFile,
    val originalElement: KtElement,
    val mutations: MutableList<Mutation> = mutableListOf()
) {
    val location: Location
    fun getText(): String = originalElement.text

    init {
        val fileContent = sourceFile.getText()
        location = Location(
            convertPosition(fileContent, originalElement.startOffset),
            convertPosition(fileContent, originalElement.endOffset)
        )
    }

    private fun convertPosition(fileContent: String, offset: Int): Position {
        var lineNum = 1
        var columnNum = offset + 1
        for ((index, line) in fileContent.split('\n').withIndex()) {
            if (columnNum - line.length < 1) {
                lineNum += index
                break
            }
            columnNum -= line.length + 1
        }

        return Position(lineNum, columnNum)
    }
}
