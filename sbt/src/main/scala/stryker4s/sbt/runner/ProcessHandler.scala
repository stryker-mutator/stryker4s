package stryker4s.sbt.runner

import scala.tools.nsc.io.Socket
import stryker4s.model.MutantRunResult
import java.nio.file.Path

class ProcessHandler() {
  def newProcess(classpath: Seq[String]) = {
    val socket = new Socket(new java.net.Socket("127.0.0.1", 13337))
    socket.outputStream().write(Array.emptyByteArray)
    val args: Seq[String] = "-cp" +: classpath

    scala.sys.process.Process("java", args)

  }

  def runTests(mutation: Option[Int], path: Path): MutantRunResult = ???
}
