package stryker4s

import java.{util => ju}

import scala.collection.mutable.{Map => MutableMap}
import scala.jdk.CollectionConverters._

object ScalaVersionCompat extends ScalaVersionCompatOps {
  override def mapAsJavaImpl[A, B](map: MutableMap[A, B]): ju.Map[A, B] = map.asJava

  override def queueAsScala[A](list: ju.Queue[A]): Iterable[A] = list.asScala
}
