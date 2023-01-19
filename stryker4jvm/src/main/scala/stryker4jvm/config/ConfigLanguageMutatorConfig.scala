package stryker4jvm.config

case class ConfigLanguageMutatorConfig(
    dialect: Option[String] = None,
    excludedMutations: Option[Config.ExcludedMutations] = None
)
