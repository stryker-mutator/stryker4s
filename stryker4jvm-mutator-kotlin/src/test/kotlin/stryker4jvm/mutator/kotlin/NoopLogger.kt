package stryker4jvm.mutator.kotlin

import stryker4jvm.core.logging.LogLevel

class NoopLogger : stryker4jvm.core.logging.Logger() {
  override fun log(p0: LogLevel?, p1: String?) {}

  override fun log(p0: LogLevel?, p1: String?, p2: Throwable?) {}

  override fun log(p0: LogLevel?, p1: Throwable?) {}
}
