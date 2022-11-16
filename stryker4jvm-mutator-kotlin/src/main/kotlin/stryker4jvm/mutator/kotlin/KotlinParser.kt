package stryker4jvm.mutator.kotlin

import stryker4jvm.core.model.Parser
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.nio.file.Path

class KotlinParser : Parser<KotlinAST> {

    override fun parse(path: Path?): KotlinAST {
        // todo: can project be left 'null' or not? Only one way to find out tho... :D
        return KotlinAST(KtPsiFactory(null).createFile(java.nio.file.Files.readString(path)))
    }


}