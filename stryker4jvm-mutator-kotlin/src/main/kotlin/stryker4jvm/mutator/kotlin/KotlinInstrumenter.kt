package stryker4jvm.mutator.kotlin

import kotlin.jvm.Throws
import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.core.exception.UnsupportedInstrumenterOptionsException
import stryker4jvm.core.model.Instrumenter
import stryker4jvm.core.model.InstrumenterOptions
import stryker4jvm.core.model.MutantWithId
import stryker4jvm.mutator.kotlin.utility.PsiUtility

class KotlinInstrumenter(private val options: KotlinInstrumenterOptions) : Instrumenter<KotlinAST> {

  @Throws(UnsupportedInstrumenterOptionsException::class)
  constructor(options: InstrumenterOptions) : this(KotlinInstrumenterOptions(options))

  constructor() : this(InstrumenterOptions.EnvVar)

  // note that in kotlin we replace the children in the original source
  // unlike scala variant
  override fun instrument(
      source: KotlinAST?,
      mutations: MutableMap<KotlinAST, List<MutantWithId<KotlinAST>>>?
  ): KotlinAST? {
    if (source == null || mutations == null) return null // or throw exception?

    mutations.forEach { (original, mutations) ->
      val whenExpression = whenExpressionGenerator(original, mutations)
      PsiUtility.replacePsiElement(original.tree, whenExpression)
    }

    // wrap in new AST instance because of how KotlinAST is defined (immutable)
    return KotlinAST(source.tree)
  }

  private fun whenExpressionGenerator(
      original: KotlinAST,
      mutations: List<MutantWithId<KotlinAST>>
  ): KtElement {
    var whenExpressionString = "${options.whenExpression} {"
    mutations.forEach { mutation ->
      whenExpressionString +=
          "\n\"${mutation.id}\" -> ${mutation.mutatedCode.mutatedStatement.tree.text}"
    }
    whenExpressionString += "\nelse -> ${original.tree.text}\n}"

    return PsiUtility.createPsiElement(whenExpressionString)
  }
}
