package stryker4jvm.mutator.scala

import stryker4jvm.core.config.LanguageMutatorConfig
import stryker4jvm.core.model.{InstrumenterOptions, LanguageMutator}
import stryker4jvm.core.model.languagemutator.LanguageMutatorProvider
import stryker4jvm.core.logging.Logger
import stryker4jvm.mutator.scala.ScalaMutatorProvider.parseDialect

import scala.meta.{Dialect, dialects};

/** Class used to actually create and provide the scala mutator to stryker4jvm
  */
class ScalaMutatorProvider extends LanguageMutatorProvider {
  override def provideMutator(
      languageMutatorConfig: LanguageMutatorConfig,
      log: Logger,
      instrumenterOptions: InstrumenterOptions
  ): LanguageMutator[ScalaAST] = {
    new ScalaMutator(
      new ScalaParser(parseDialect(languageMutatorConfig.getDialect, log)),
      new ScalaCollector(
        traverser = new TraverserImpl()(log),
        matcher = new MutantMatcherImpl(config = languageMutatorConfig)
      )(log),
      new ScalaInstrumenter(
        options = ScalaInstrumenterOptions.fromJavaOptions(instrumenterOptions = instrumenterOptions)
      )
    )
  }
}

object ScalaMutatorProvider {
  def parseDialect(configDialect: String, log: Logger): Dialect = {
    val deprecatedVersions = List("scala211", "scala2.11", "2.11", "211")
    if (deprecatedVersions.contains(configDialect)) {
      log.warn("Using deprecated Scala dialect")
      return dialects.Scala211
    }

    val defaultVersion = dialects.Scala213
    val scalaVersions = Map(
      List("scala212", "scala2.12", "2.12", "212") -> dialects.Scala212,
      List("scala212source3") -> dialects.Scala212Source3,
      List("scala213", "scala2.13", "2.13", "213", "2") -> dialects.Scala213,
      List("scala213source3", "source3") -> dialects.Scala213Source3,
      List("scala3future", "future") -> dialects.Scala3Future,
      List("scala30", "scala3.0", "3.0", "30", "dotty") -> dialects.Scala30,
      List("scala31", "scala3.1", "3.1", "31") -> dialects.Scala31,
      List("scala32", "scala3.2", "3.2", "32") -> dialects.Scala32,
      List("scala3", "scala3.0", "3.0", "3") -> dialects.Scala3
    )
    val dialectMap = scalaVersions.flatMap{case (versions, dialect) => {
      versions.map(v => v -> dialect)
    }}
    dialectMap.get(configDialect) match {
      case Some(dialect) => dialect
      case None => {
        log.warn(s"Unknown Scala dialect $configDialect, using default dialect $defaultVersion")
        defaultVersion
      }
    }
  }
}
