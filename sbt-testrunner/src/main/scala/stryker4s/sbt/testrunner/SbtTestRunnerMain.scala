package stryker4s.sbt.testrunner

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}

import stryker4s.api.testprocess.{Request, TestProcessConfig}

object SbtTestRunnerMain {
  def main(args: Array[String]): Unit = {
    println("Started testrunner")
    val config = Context.resolveSocketConfig()
    setupSocketServer(config)
  }

  private def setupSocketServer(config: TestProcessConfig) = {
    println("Setting up server")
    val server = new ServerSocket(config.port)
    try {
      val socket = server.accept()
      try {

        println(s"Ready to accept connections on port ${config.port}")

        val messageHandler = new TestRunnerMessageHandler()
        val server = new TestProcessServer(messageHandler, socket)
        server.start()
      } finally {
        socket.close()
      }
    } finally {
      server.close()
    }
  }
}

final class TestProcessServer(messageHandler: MessageHandler, socket: Socket) {

  def start() = {
    val objectInputStream = new ObjectInputStream(socket.getInputStream())
    try {
      val objectOutputStream = new ObjectOutputStream(socket.getOutputStream())
      try {

        while (true) {
          objectInputStream.readObject() match {
            case request: Request =>
              println(s"Received message $request")
              val response = messageHandler.handleMessage(request)
              objectOutputStream.writeObject(response)
            case other => throw new Exception(s"Could not handle message. Expected type 'Request', but received $other")
          }
        }
      } finally {
        objectOutputStream.close()
      }
    } finally {
      objectInputStream.close()
    }
  }
}
