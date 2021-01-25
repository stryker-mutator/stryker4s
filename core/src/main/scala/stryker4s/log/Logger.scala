package stryker4s.log

trait Logger {
  final def debug(msg: => String): Unit = log(Debug, msg)
  final def debug(msg: => String, e: Throwable): Unit = log(Debug, msg, e)
  final def debug(e: Throwable): Unit = log(Debug, e)

  final def info(msg: => String): Unit = log(Info, msg)
  final def info(msg: => String, e: Throwable): Unit = log(Info, msg, e)
  final def info(e: Throwable): Unit = log(Info, e)

  final def warn(msg: => String): Unit = log(Warn, msg)
  final def warn(msg: => String, e: Throwable): Unit = log(Warn, msg, e)
  final def warn(e: Throwable): Unit = log(Warn, e)

  final def error(msg: => String): Unit = log(Error, msg)
  final def error(msg: => String, e: Throwable): Unit = log(Error, msg, e)
  final def error(e: Throwable): Unit = log(Error, e)

  def log(level: Level, msg: => String): Unit
  def log(level: Level, msg: => String, e: => Throwable): Unit
  def log(level: Level, e: Throwable): Unit
}
