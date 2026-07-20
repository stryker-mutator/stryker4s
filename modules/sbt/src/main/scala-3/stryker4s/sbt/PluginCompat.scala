package stryker4s.sbt

import _root_.sbt.*

import java.nio.file.Path
import scala.collection.mutable

/** For compatibility with sbt 2.x
  */
private[stryker4s] object PluginCompat {

  def toNioPaths(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Seq[Path] =
    cp.map(a => conv.toPath(a.data))

  def runTask[T](taskKey: TaskKey[T], state: State): Option[Either[Incomplete, T]] =
    // Detach to avoid errors bubbling up
    val detachedState = state.copy(currentCommand = state.currentCommand.map(_.withSource(None)))
    val extracted = Project.extract(detachedState)
    val config = EvaluateTask.extractedTaskConfig(extracted, extracted.structure, detachedState)
    EvaluateTask(extracted.structure, taskKey.scopedKey, detachedState, extracted.currentRef, config)
      .map(_._2.toEither)

  def mapValues[K, V, W](map: mutable.Map[K, V])(f: V => W): Map[K, W] =
    map.view.mapValues(f).toMap
}
