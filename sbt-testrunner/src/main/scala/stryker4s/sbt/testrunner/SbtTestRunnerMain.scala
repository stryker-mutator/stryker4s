package stryker4s.sbt.testrunner

import com.google.protobuf.CodedInputStream
import stryker4s.api.testprocess.RequestMessage
import stryker4s.logTimed

import java.net.{InetAddress, ServerSocket, Socket}

object SbtTestRunnerMain {
  def main(args: Array[String]): Unit = {
    println("Started testrunner")
    val port = Context.resolveSocketConfig()
    setupSocketServer(port)
  }

  private def setupSocketServer(port: Int) = {
    println(s"Setting up server on port $port")
    val server = new ServerSocket(port, 0, InetAddress.getLoopbackAddress)
    try {
      val socket = server.accept()
      try {

        println(s"Ready to accept connections on port $port")

        val messageHandler = new TestRunnerMessageHandler()
        val server = new TestProcessServer(messageHandler, socket)
        server.start()
      } finally socket.close()
    } finally server.close()
  }
}

final class TestProcessServer(messageHandler: MessageHandler, socket: Socket) {

  def start() = {
    val inputStream = socket.getInputStream()
    val input = CodedInputStream.newInstance(inputStream)
    try {
      val outputStream = socket.getOutputStream()
      try while (true) {
        val request = logTimed("TestRunnerReadMessage")(RequestMessage.parseDelimitedFrom(input).get.toRequest)
        println(s"Received message $request")
        val response = messageHandler.handleMessage(request)
        logTimed("TestRunnerSendMessage")(response.asMessage.writeDelimitedTo(outputStream))
      } finally outputStream.close()
    } finally inputStream.close()
  }
}
