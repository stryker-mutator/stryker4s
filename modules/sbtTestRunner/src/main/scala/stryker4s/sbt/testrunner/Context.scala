package stryker4s.sbt.testrunner

import stryker4s.testrunner.api.TestProcessProperties

object Context {
  def resolveSocketConfig(): SocketConfig = {
    def unixSocketConfig(): Option[SocketConfig.UnixSocket] =
      sys.props
        .get(TestProcessProperties.unixSocketPath)
        .map(SocketConfig.UnixSocket(_))

    def tcpSocketConfig(): Option[SocketConfig.TcpSocket] =
      sys.props
        .get(TestProcessProperties.port)
        .map(_.toInt)
        .map(SocketConfig.TcpSocket(_))

    unixSocketConfig()
      .orElse(tcpSocketConfig())
      .getOrElse(throw new IllegalStateException("No socket configuration found in system properties"))
  }
}

sealed trait SocketConfig

object SocketConfig {
  case class UnixSocket(path: String) extends SocketConfig
  case class TcpSocket(port: Int) extends SocketConfig
}
