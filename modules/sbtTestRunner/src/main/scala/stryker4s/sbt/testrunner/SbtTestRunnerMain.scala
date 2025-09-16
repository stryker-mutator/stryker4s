package stryker4s.sbt.testrunner

import stryker4s.sbt.testrunner.SocketConfig.{TcpSocket, UnixSocket}
import stryker4s.sbt.testrunner.server.{TcpSocketServer, UnixSocketServer}

object SbtTestRunnerMain {
  def main(args: Array[String]): Unit = {
    println("Started testrunner")
    val config = Context.resolveSocketConfig()

    config match {
      case UnixSocket(path) => UnixSocketServer.start(path)
      case TcpSocket(port)  => TcpSocketServer.start(port)
    }
  }

}
