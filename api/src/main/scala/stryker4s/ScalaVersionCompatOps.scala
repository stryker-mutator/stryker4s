package stryker4s

import java.{util => ju}
import java.util.HashMap
import scala.collection.mutable.{Map => MutableMap}

private[stryker4s] trait ScalaVersionCompatOps {

  // Wrap in a HashMap to get rid of the Scala wrapper proxy which does not serialize well
  def mapAsJava[A, B](map: MutableMap[A, B]): ju.Map[A, B] = new HashMap(mapAsJavaImpl(map))

  protected def mapAsJavaImpl[A, B](map: MutableMap[A, B]): ju.Map[A, B]

  def queueAsScala[A](list: ju.Queue[A]): Iterable[A]
}
