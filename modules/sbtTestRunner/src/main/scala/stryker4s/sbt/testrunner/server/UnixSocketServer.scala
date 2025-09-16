package stryker4s.sbt.testrunner.server

import com.google.protobuf.{CodedInputStream, CodedOutputStream}
import stryker4s.sbt.testrunner.{MessageHandler, TestRunnerMessageHandler}
import stryker4s.testrunner.api.{Request, RequestMessage, Response}

import java.io.{ByteArrayOutputStream, File, IOException}
import java.net.{StandardProtocolFamily, UnixDomainSocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import scala.annotation.tailrec

object UnixSocketServer {
  def start(socketPath: String): Unit = {
    println(s"Setting up socket server at $socketPath")
    val socketFile = new File(socketPath)
    socketFile.deleteOnExit()

    val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
    try {
      channel.configureBlocking(false)

      channel.bind(UnixDomainSocketAddress.of(socketFile.toPath()))
      val sel = Selector.open()

      try {
        channel.register(sel, SelectionKey.OP_ACCEPT, new ServerActor(channel, sel))
        println(s"Listening on $socketPath")
        while (sel.select() > 0) {
          val keys = sel.selectedKeys()
          val iterator = keys.iterator()
          var running = false
          var cancelled = false
          while (iterator.hasNext()) {
            val k = iterator.next()
            val a = k.attachment().asInstanceOf[Actor]
            if (a.rxready()) {
              running = true
            } else {
              k.cancel()
              cancelled = true
            }
            iterator.remove()
          }
          if (!running && cancelled) {
            println("No more connections, shutting down")
            return
          }
        }
      } finally sel.close()
    } finally channel.close()
  }

}

/** An actor is something that can do something when it is ready to read.
  */
trait Actor {
  def rxready(): Boolean
}

/** Accepts new connections and registers them with the selector.
  */
final class ServerActor(channel: ServerSocketChannel, selector: Selector) extends Actor {

  override def rxready(): Boolean = {
    try {
      val client = channel.accept()
      client.configureBlocking(false)
      client.register(selector, SelectionKey.OP_READ, new ClientActor(client, new TestRunnerMessageHandler()))
      true
    } catch {
      case _: IOException => false
    }
  }
}

/** Handles communication with a single client.
  */
final class ClientActor(channel: SocketChannel, messageHandler: MessageHandler) extends Actor {

  override def rxready(): Boolean = {
    try {
      read().foreach { request =>
        println(s"Received message $request")
        val response = messageHandler.handleMessage(request)
        write(response)
      }
      true
    } catch {
      case e: IOException =>
        e.printStackTrace()
        false
    }
  }

  private def read(): Option[Request] = {
    def readLengthTag(): Option[Int] = {
      @tailrec
      def go(acc: Array[Byte]): Option[Array[Byte]] = readN(1) match {
        case Some(newByte) if hasReadMoreTag(newByte) => go(acc ++ newByte)
        case Some(newByte)                            => Some(acc ++ newByte)
        case None                                     => None
      }

      go(Array.emptyByteArray).map(readRawVarint32)
    }

    for {
      incomingMessageLength <- readLengthTag()
      messageBytes <- readN(incomingMessageLength)
    } yield RequestMessage.parseFrom(messageBytes).toRequest
  }

  /** Read exactly `size` bytes from the channel, or throw an IOException
    */
  private def readN(size: Int): Option[Array[Byte]] = {
    val buf = ByteBuffer.allocate(size)
    val bytesRead = channel.read(buf)

    if (bytesRead < 0) None
    else if (bytesRead != size) throw new IOException(s"Expected to read $size bytes, but read $bytesRead bytes")
    else Some(buf.array())
  }

  private def write(response: Response): Unit = {
    val msg = response.asMessage

    val serialized: Int = msg.serializedSize
    val bufferSize: Int = CodedOutputStream.computeUInt32SizeNoTag(serialized) + serialized
    val buf = ByteBuffer.allocate(bufferSize)
    val out = new ByteArrayOutputStream(bufferSize)

    msg.writeDelimitedTo(out)
    buf.put(out.toByteArray)
    buf.flip()
    writeBuffer(buf)
  }

  @tailrec
  private def writeBuffer(buffer: ByteBuffer): Unit = {
    val written = channel.write(buffer)
    if (written < 0) {
      throw new IOException("Could not write to socket")
    } else if (written >= 0 && buffer.hasRemaining) {
      writeBuffer(buffer)
    }
  }

  /** Check if the most significant bit is set, indicating that there are more bytes to read for the delimiter.
    */
  private def hasReadMoreTag(bytes: Array[Byte]): Boolean = bytes.lastOption.forall(byte => (byte & 0x80) != 0)

  private def readRawVarint32(bv: Array[Byte]): Int = CodedInputStream.newInstance(bv).readRawVarint32()

}
