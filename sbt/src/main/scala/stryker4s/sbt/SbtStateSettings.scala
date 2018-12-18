package stryker4s.sbt
import java.io.{BufferedOutputStream, PrintStream}

import sbt.{ConsoleOut, CustomOutput, ForkOptions}
import sbt.Keys.{fork, forkOptions, logManager}
import sbt._
import sbt.internal.LogManager

object SbtStateSettings {

  /**
    *  Include these settings to prevent all test logging
    */
  val noLoggingSettings = Seq(
    fork in Test := true,
    forkOptions in Test := ForkOptions().withOutputStrategy(Some(CustomOutput(
      new BufferedOutputStream((_: Int) => {}) // Empty stream
    ))),
    logManager in Test := LogManager.defaultManager(ConsoleOut.printStreamOut(
      new PrintStream((_: Int) => {}) // Empty stream
    ))
  )
}
