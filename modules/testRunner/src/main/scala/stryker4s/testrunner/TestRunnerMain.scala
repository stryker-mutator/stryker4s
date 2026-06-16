package stryker4s.testrunner

import stryker4s.testrunner.SocketConfig.{TcpSocket, UnixSocket}
import stryker4s.testrunner.server.{TcpSocketServer, UnixSocketServer}

object TestRunnerMain {
  def main(args: Array[String]): Unit = {
    println("Started testrunner")
    val config = Context.resolveSocketConfig()

    config match {
      case UnixSocket(path) => UnixSocketServer.start(path)
      case TcpSocket(port)  => TcpSocketServer.start(port)
    }
  }

}
