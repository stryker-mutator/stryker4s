import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

import sbt._
import sbt.internal.util.ManagedLogger

import scala.collection.JavaConverters._
import scala.util.Properties

/** Starting point: https://github.com/randomcoder/sbt-git-hooks/blob/master/src/main/scala/uk/co/randomcoding/sbt/GitHooks.scala
  */
object GitHooks {
  def apply(hooksSourceDir: File, hooksTargetDir: File, log: ManagedLogger): Unit =
    if (hooksSourceDir.isDirectory && hooksTargetDir.exists()) {
      IO.listFiles(hooksSourceDir)
        .map(hook => (hook, hooksTargetDir / hook.name))
        .filterNot(_._2.exists()) // Don't write if hook already exists
        .foreach { case (originalHook, targetHook) =>
          log.info(s"Copying ${originalHook.name} hook to $targetHook")
          Files.copy(originalHook.asPath, targetHook.asPath)
          if (!Properties.isWin)
            targetHook.setPermissions(PosixFilePermissions.fromString("rwxr-xr-x").asScala.toSet)
        }
    }
}
