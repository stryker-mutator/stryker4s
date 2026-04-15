package stryker4s.sbt

import _root_.sbt.*

import java.nio.file.Path as NioPath
import scala.collection.mutable

/** For compatibility with sbt 2.x
  */
private[stryker4s] object PluginCompat {

  def toNioPaths(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Vector[NioPath] =
    cp.map(a => conv.toPath(a.data)).toVector

  def runTask[T](taskKey: TaskKey[T], state: State): Option[Either[Incomplete, T]] =
    val extracted = Project.extract(state)
    val config = EvaluateTask.extractedTaskConfig(extracted, extracted.structure, state)
    EvaluateTask(extracted.structure, taskKey.scopedKey, state, extracted.currentRef, config).map(_._2.toEither)

  def mapValues[K, V, W](map: mutable.Map[K, V])(f: V => W): Map[K, W] =
    map.view.mapValues(f).toMap
}
