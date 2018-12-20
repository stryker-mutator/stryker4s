package stryker4s.sbt

import grizzled.slf4j.Logging
import sbt.Keys.fork
import sbt._

object SbtStateSettings extends Logging {

  /**
    *  Include these settings to prevent all test logging
    */
  def noLoggingSettings(envVars : scala.collection.immutable.Map[_root_.scala.Predef.String, _root_.scala.Predef.String] = Map()) = Seq(
    fork in Test := true,
//    forkOptions in Test := {
//      ForkOptions().withEnvVars(envVars).withOutputStrategy(Some(CustomOutput(
//        new BufferedOutputStream((_: Int) => {}))))
//    },
//    logManager in Test := LogManager.defaultManager(ConsoleOut.printWriterOut(
//      new PrintWriter((_: Int) => {}) // Empty stream
//    ))
  )
}
