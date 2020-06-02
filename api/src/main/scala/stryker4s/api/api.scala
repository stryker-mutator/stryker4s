package stryker4s.api

import java.rmi.Remote

sealed trait Send extends Remote {
  def send(message: Message): Response
}

sealed trait Response

sealed trait Message
