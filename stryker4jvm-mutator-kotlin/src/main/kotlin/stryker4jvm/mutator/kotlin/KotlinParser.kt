package stryker4jvm.mutator.kotlin

import java.nio.file.Path
import stryker4jvm.core.model.Parser
import stryker4jvm.mutator.kotlin.utility.PsiUtility

class KotlinParser : Parser<KotlinAST> {

  override fun parse(path: Path?): KotlinAST {
    return KotlinAST(PsiUtility.createPsiFile(java.nio.file.Files.readString(path)))
  }
}
