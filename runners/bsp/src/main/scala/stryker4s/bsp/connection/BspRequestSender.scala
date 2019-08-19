package stryker4s.bsp.connection

import ch.epfl.scala.bsp.StatusCode

trait BspRequestSender[T] {
  def apply(args: String): StatusCode
}

trait Request

trait TestRequest extends Request

object BspRequestSender {

  implicit def testRequestSender(request: String): BspRequestSender[TestRequest] = (_: String) => ???
}
