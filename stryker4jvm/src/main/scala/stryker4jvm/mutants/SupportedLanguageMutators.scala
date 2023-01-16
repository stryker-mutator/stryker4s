package stryker4jvm.mutants

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.{AST, InstrumenterOptions, LanguageMutator}
import stryker4jvm.core.model.languagemutator.LanguageMutatorProvider
import stryker4jvm.mutator.kotlin.KotlinMutatorProvider
import stryker4jvm.mutator.scala.ScalaMutatorProvider
import stryker4jvm.logging.FansiLogger

case class SupportedProvider(
    name: String,
    directory: String,
    extension: String,
    languageProvider: LanguageMutatorProvider
) {
  def provideMutator(
      config: LanguageMutatorConfig,
      logger: FansiLogger,
      options: InstrumenterOptions
  ): LanguageMutator[? <: AST] =
    languageProvider.provideMutator(config, logger.coreLogger, options).asInstanceOf[LanguageMutator[? <: AST]]
}

object SupportedLanguageMutators {
  val supportedProviders: Seq[SupportedProvider] = Seq(
    SupportedProvider("kotlin", "kotlin", "kt", new KotlinMutatorProvider),
    SupportedProvider("scala", "scala", "scala", new ScalaMutatorProvider)
  )

  def supportedMutators(
      configs: Map[String, LanguageMutatorConfig],
      logger: FansiLogger,
      options: InstrumenterOptions
  ): Map[String, LanguageMutator[? <: AST]] = {
    val default = new LanguageMutatorConfig(new java.util.HashSet())
    supportedProviders
      .map(provider =>
        try
          Some(
            "." + provider.extension -> provider
              .provideMutator(configs.getOrElse(provider.name, default), logger, options)
          )
        catch {
          case e: Throwable =>
            logger.warn(
              s"Language mutator provider '${provider.getClass.getName}' threw an exception with message ${e.getMessage}; this mutator will be ignored."
            )
            None
        }
      )
      .filter(_.isDefined)
      .map(_.get)
      .toMap
  }

  def mutatesFileSources: Seq[String] =
    supportedProviders.map(mutator => s"**/main/${mutator.directory}/**.${mutator.extension}")
}
