package stryker4s.sbt.testrunner

import stryker4s.api.testprocess.Request

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.net.{ServerSocket, Socket}

object SbtTestRunnerMain {
  def main(args: Array[String]): Unit = {
    println("Started testrunner")
    val port = Context.resolveSocketConfig()
    setupSocketServer(port)
  }

  private def setupSocketServer(port: Int) = {
    println(s"Setting up server on port ${port}")
    val server = new ServerSocket(port)
    try {
      val socket = server.accept()
      try {

        println(s"Ready to accept connections on port ${port}")

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
