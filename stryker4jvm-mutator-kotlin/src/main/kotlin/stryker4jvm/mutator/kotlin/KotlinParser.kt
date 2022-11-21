package stryker4jvm.mutator.kotlin

import stryker4jvm.core.model.Parser
import org.jetbrains.kotlin.psi.KtPsiFactory
import stryker4jvm.mutator.kotlin.utility.PsiUtility
import java.nio.file.Path

class KotlinParser : Parser<KotlinAST> {

    override fun parse(path: Path?): KotlinAST {
        return KotlinAST(PsiUtility.createPsiFile(java.nio.file.Files.readString(path)))
    }
}