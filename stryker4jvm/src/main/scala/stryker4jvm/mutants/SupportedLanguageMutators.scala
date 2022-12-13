package stryker4jvm.mutants

import stryker4jvm.core.model.LanguageMutator
import stryker4jvm.core.model.AST
import stryker4jvm.mutator.kotlin.KotlinMutator

case class SupportedMutator(directory: String, extension: String, languageMutator: LanguageMutator[? <: AST])

object SupportedLanguageMutators {
  val supportedMutators: Array[SupportedMutator] = Array(
    SupportedMutator("kotlin", "kt", new KotlinMutator())
  )

  def languageRouter: Map[String, LanguageMutator[? <: AST]] =
    supportedMutators.map(mutator => mutator.extension -> mutator.languageMutator).toMap

  def mutatesFileSources: Seq[String] =
    supportedMutators.map(mutator => s"**/main/${mutator.directory}/**.${mutator.extension}")
}
