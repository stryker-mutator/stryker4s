package stryker4s.env

trait Environment {
  def getEnvVariable(key: String): Option[String]
}

object SystemEnvironment extends Environment {
  override def getEnvVariable(key: String): Option[String] = sys.env.get(key)
}
