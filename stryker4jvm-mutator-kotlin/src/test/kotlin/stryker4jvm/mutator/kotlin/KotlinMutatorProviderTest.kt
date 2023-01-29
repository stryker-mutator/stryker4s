package stryker4jvm.mutator.kotlin

import kotlin.test.Test
import kotlin.test.assertEquals
import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.InstrumenterOptions
import stryker4jvm.core.model.LanguageMutator
import stryker4jvm.mutator.kotlin.mutators.BooleanLiteralMutator
import stryker4jvm.mutator.kotlin.utility.PsiUtility

class KotlinMutatorProviderTest {
  private val provider = KotlinMutatorProvider()

  @Test
  fun testSimpleProvider() {
    val config = LanguageMutatorConfig("Kotlin", HashSet())
    val logger = NoopLogger()
    val options = InstrumenterOptions.SysProp
    val mutator = provider.provideMutator(config, logger, options)
    testBooleanLiteral(mutator, true)
  }

  @Test
  fun testConfiguredProvider() {
    val config = LanguageMutatorConfig(null, setOf(BooleanLiteralMutator.name))
    val logger = NoopLogger()
    val options = InstrumenterOptions.EnvVar
    val mutator = provider.provideMutator(config, logger, options)
    testBooleanLiteral(mutator, false)
  }

  private fun testBooleanLiteral(mutator: LanguageMutator<*>, expect: Boolean) {
    val booleanLiteral = KotlinAST(PsiUtility.createPsiElement("false"))
    assertEquals(expect, mutator.collect(booleanLiteral).mutations.isNotEmpty())
  }
}
