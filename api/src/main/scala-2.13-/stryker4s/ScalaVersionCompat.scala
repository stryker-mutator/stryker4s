package stryker4s

import java.{util => ju}
import scala.collection.JavaConverters._

object ScalaVersionCompat extends ScalaVersionCompatOps {
  override def queueAsScala[A](list: ju.Queue[A]): Iterable[A] = list.asScala
}
