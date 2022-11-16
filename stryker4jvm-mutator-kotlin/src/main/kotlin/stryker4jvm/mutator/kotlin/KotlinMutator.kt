package stryker4jvm.mutator.kotlin

import org.jetbrains.kotlin.psi.KtElement
import stryker4jvm.core.model.LanguageMutator

/*
 * Issue here:
 * for some reason, LanguageMutator does not enforce the generic to be a subclass of AST
 * I can simply use KtElement directly even though it does not contain a 'syntax()' method
 */
class KotlinMutator : LanguageMutator<KotlinAST>(KotlinParser(), MutationCollector(), KotlinInstrumenter()) {

}