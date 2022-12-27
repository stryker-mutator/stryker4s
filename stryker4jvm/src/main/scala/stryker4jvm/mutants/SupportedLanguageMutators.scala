package stryker4jvm.mutants

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.{AST, InstrumenterOptions, LanguageMutator}
import stryker4jvm.core.model.languagemutator.LanguageMutatorProvider
import stryker4jvm.mutator.kotlin.KotlinMutatorProvider
import stryker4jvm.mutator.scala.ScalaMutatorProvider

case class SupportedProvider(
    name: String,
    directory: String,
    extension: String,
    languageProvider: LanguageMutatorProvider
) {
  def provideMutator(config: LanguageMutatorConfig, options: InstrumenterOptions): LanguageMutator[? <: AST] =
    languageProvider.provideMutator(config, options).asInstanceOf[LanguageMutator[? <: AST]]
}

object SupportedLanguageMutators {
  val supportedProviders: Seq[SupportedProvider] = Seq(
    SupportedProvider("kotlin", "kotlin", "kt", new KotlinMutatorProvider),
    SupportedProvider("scala", "scala", "scala", new ScalaMutatorProvider)
  )

  def supportedMutators(
      configs: Map[String, LanguageMutatorConfig],
      options: InstrumenterOptions
  ): Map[String, LanguageMutator[? <: AST]] = {
    val default = new LanguageMutatorConfig(new java.util.HashSet())
    supportedProviders
      .map(provider =>
        provider.extension -> provider.provideMutator(configs.getOrElse(provider.name, default), options)
      )
      .toMap
  }

  def mutatesFileSources: Seq[String] =
    supportedProviders.map(mutator => s"**/main/${mutator.directory}/**.${mutator.extension}")
}
