package stryker4s

import java.{util => ju}

private[stryker4s] trait ScalaVersionCompatOps {

  def queueAsScala[A](list: ju.Queue[A]): Iterable[A]

}
