package stryker4s.sbt

import _root_.sbt.*
import xsbti.FileConverter

import java.io.File
import java.nio.file.Path as NioPath
import scala.annotation.nowarn
import scala.collection.mutable

/** For compatibility with sbt 1.x
  */
private[stryker4s] object PluginCompat {

  @nowarn("msg=parameter value conv in method toNioPath is never used")
  def toNioPath(a: Attributed[File])(implicit conv: FileConverter): NioPath =
    a.data.toPath()

  @nowarn("msg=parameter value conv in method toNioPaths is never used")
  def toNioPaths(cp: Seq[Attributed[File]])(implicit conv: FileConverter): Vector[NioPath] =
    cp.map(_.data.toPath()).toVector

  def runTask[T](taskKey: TaskKey[T], state: State): Option[Either[Incomplete, T]] =
    Project.runTask(taskKey, state).map(_._2.toEither)

  def mapValues[K, V, W](map: mutable.Map[K, V])(f: V => W): Map[K, W] =
    map.mapValues(f).toMap
}
