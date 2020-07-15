package stryker4s.sbt.testrunner

import stryker4s.api.testprocess.TestProcessConfig
import java.net.ServerSocket
import java.net.Socket
import stryker4s.api.testprocess.Request
import java.io.ObjectOutputStream
import java.io.ObjectInputStream

object SbtTestRunnerMain {
  def main(args: Array[String]): Unit = {
    println("Started testrunner")
    val config = Context.resolveSocketConfig()
    setupSocketServer(config).start()
    ()
  }

  private def setupSocketServer(config: TestProcessConfig): TestProcessServer = {
    println("Setting up server")
    val server = new ServerSocket(config.port)
    val socket = server.accept()

    println(s"Ready to accept connections on port ${config.port}")

    val messageHandler = new CleanMessageHandler()
    new TestProcessServer(messageHandler, socket)
  }
}

final class TestProcessServer(messageHandler: MessageHandler, socket: Socket) {
  val objectOutputStream = new ObjectOutputStream(socket.getOutputStream())
  val objectInputStream = new ObjectInputStream(socket.getInputStream())

  def start() = {
    var handler = messageHandler
    while (true) {
      objectInputStream.readObject() match {
        case request: Request =>
          println(s"Received message $request")
          val newHandler = handler.setupState(request)
          val response = handler.handleMessage(request)
          handler = newHandler
          objectOutputStream.writeObject(response)
        case other => throw new Exception(s"Could not handle message. Expected type 'Request', but received $other")
      }
    }
  }
}
