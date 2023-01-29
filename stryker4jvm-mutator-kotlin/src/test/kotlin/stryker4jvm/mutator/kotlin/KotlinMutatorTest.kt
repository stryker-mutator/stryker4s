package stryker4jvm.mutator.kotlin

import kotlin.test.Test
import kotlin.test.assertEquals
import stryker4jvm.mutator.kotlin.mutators.BooleanLiteralMutator
import stryker4jvm.mutator.kotlin.mutators.EqualityOperatorMutator
import stryker4jvm.mutator.kotlin.mutators.StringLiteralMutator
import stryker4jvm.mutator.kotlin.utility.PsiUtility

class KotlinMutatorTest {
  @Test
  fun testDefaultConstructor() {
    val mutator = KotlinMutator()
    testBooleanLiteral(mutator, true)
    testConditionalExpression(mutator, true)
    testEqualityOperator(mutator, true)
    testLogicalOperator(mutator, true)
    testStringLiteral(mutator, true)
  }

  @Test
  fun testSecondaryConstructor() {
    var mutator = KotlinMutator(KotlinCollector(arrayOf(BooleanLiteralMutator)))
    testBooleanLiteral(mutator, true)
    testConditionalExpression(mutator, false)
    testEqualityOperator(mutator, false)
    testLogicalOperator(mutator, false)
    testStringLiteral(mutator, false)

    mutator = KotlinMutator(KotlinCollector(arrayOf(StringLiteralMutator, EqualityOperatorMutator)))
    testBooleanLiteral(mutator, false)
    testConditionalExpression(mutator, true) // encapsulated by equality operator
    testEqualityOperator(mutator, true)
    testLogicalOperator(mutator, false)
    testStringLiteral(mutator, true)
  }

  private fun testBooleanLiteral(mutator: KotlinMutator, expect: Boolean) {
    val booleanLiteral = ast("false")
    assertEquals(expect, mutator.collect(booleanLiteral).mutations.isNotEmpty())
  }

  private fun testConditionalExpression(mutator: KotlinMutator, expect: Boolean) {
    val condition = ast("if(0 < 1) print(\"a\")")
    assertEquals(expect, mutator.collect(condition).mutations.isNotEmpty())
  }

  private fun testEqualityOperator(mutator: KotlinMutator, expect: Boolean) {
    val eq = ast("if (0 === 1)")
    assertEquals(expect, mutator.collect(eq).mutations.isNotEmpty())
  }

  private fun testLogicalOperator(mutator: KotlinMutator, expect: Boolean) {
    val logic = ast("if(0 || 1)")
    assertEquals(expect, mutator.collect(logic).mutations.isNotEmpty())
  }

  private fun testStringLiteral(mutator: KotlinMutator, expect: Boolean) {
    val eq = ast("\"hello!\"")
    assertEquals(expect, mutator.collect(eq).mutations.isNotEmpty())
  }

  private fun ast(expr: String): KotlinAST {
    return KotlinAST(PsiUtility.createPsiElement(expr))
  }
}
