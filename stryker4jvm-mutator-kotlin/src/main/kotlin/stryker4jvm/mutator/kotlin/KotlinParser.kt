package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import stryker4jvm.mutants.language.Parser
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.nio.file.Path

class KotlinParser : Parser<KtElement> {
    override fun apply(path: Path?): KtElement {
        // todo: can project be left 'null' or not? Only one way to find out tho... :D
        return KtPsiFactory(null).createFile(java.nio.file.Files.readString(path))
    }
}