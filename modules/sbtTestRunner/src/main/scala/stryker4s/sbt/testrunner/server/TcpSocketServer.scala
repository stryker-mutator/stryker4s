package stryker4s.sbt.testrunner.server

import stryker4s.sbt.testrunner.TestRunnerMessageHandler
import stryker4s.testrunner.api.RequestMessage

import java.net.{InetAddress, ServerSocket}
import scala.util.Using

object TcpSocketServer {
  def start(port: Int): Unit = {
    println(s"Setting up server on port $port")

    val server = new ServerSocket(port, 0, InetAddress.getLoopbackAddress)
    Using.resources(server, server.accept()) { (_, socket) =>
      println(s"Listening on port $port")

      val inputStream = socket.getInputStream()
      val outputStream = socket.getOutputStream()

      val messageHandler = new TestRunnerMessageHandler()
      var continue = true
      while (continue)
        RequestMessage.parseDelimitedFrom(inputStream) match {
          case Some(requestMsg) =>
            val request = requestMsg.toRequest
            println(s"Received message $request")
            val response = messageHandler.handleMessage(request)
            response.asMessage.writeDelimitedTo(outputStream)
          case None =>
            println("No more messages received or failed to parse message. Closing connection.")
            continue = false
        }
    }
  }
}
