package stryker4s.bsp

import ch.epfl.scala.bsp.{
  BspConnectionDetails,
  BuildClientCapabilities,
  BuildTargetDataKind,
  InitializeBuildParams,
  StatusCode,
  Uri
}
import stryker4s.bsp.connection.BspRequestSender
import stryker4s.config.Config

import scala.meta.jsonrpc.BaseProtocolMessage

case class BspContext(connection: BspConnection) {
  def send[T](args: String)(implicit sender: BspRequestSender[T]): StatusCode = sender(args)
}

case class BspConnection()

object BspContext {

  def retrieveContext(implicit config: Config): BspContext = {
    val connection = setupConnection
    new BspContext(connection)
  }

  private def setupConnection(implicit config: Config): BspConnection = {
    import io.circe.syntax._
    val displayName = "Stryker4s"
    val version = "1.0.0"
    val bspVersion = "2.0.0"
    val rootUri = Uri(config.baseDir.uri)
    val languages = List(BuildTargetDataKind.Scala)
    val capabilities = BuildClientCapabilities(languages)
    val data = None
    val method = "build/initialize"
    val initBuildRequest = InitializeBuildParams(displayName, version, bspVersion, rootUri, capabilities, data)
    val message = BaseProtocolMessage.fromJson(initBuildRequest.asJson)
    scala.meta.jsonrpc.MessageWriter.write(message)

    BspConnectionDetails(
      name = "Stryker4s",
      argv = List("stryker4s", "bsp"),
      version = "1.0.0",
      bspVersion = "2.0.0",
      languages = List("scala")
    )
    ???
  }
}
