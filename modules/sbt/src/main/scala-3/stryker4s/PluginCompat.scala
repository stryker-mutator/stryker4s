package stryker4s

import _root_.sbt.*
import xsbti.{FileConverter, HashedVirtualFileRef, VirtualFile}

import java.nio.file.Path as NioPath

/** For compatibility with sbt 2.x
  */
private[stryker4s] object PluginCompat {
  type FileRef = java.io.File
  type Out = java.io.File

  def toNioPath(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): NioPath =
    conv.toPath(a.data)
  inline def toFile(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): File =
    toNioPath(a).toFile()
  def toNioPaths(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Vector[NioPath] =
    cp.map(toNioPath).toVector
  inline def toFiles(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Vector[File] =
    toNioPaths(cp).map(_.toFile())

  def runTask[T](taskKey: TaskKey[T], state: State): Option[Either[Incomplete, T]] =
    val extracted = Project.extract(state)
    val config = EvaluateTask.extractedTaskConfig(extracted, extracted.structure, state)
    EvaluateTask(extracted.structure, taskKey.scopedKey, state, extracted.currentRef, config).map(_._2.toEither)

}
