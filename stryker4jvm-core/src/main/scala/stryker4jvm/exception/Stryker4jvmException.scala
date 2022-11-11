package stryker4jvm.exception

abstract class Stryker4jvmException(message: String) extends Exception(message) {
  def this(message: String, cause: Throwable) = {
    this(message)
    initCause(cause)
  }
}
