import sbt.Keys._
import sbt._

import scala.sys.process

object Release {

  // Main release commands
  private val stryker4sPublish = "stryker4sPublish"
  private val stryker4sPublishSigned = "stryker4sPublishSigned"
  // Helper command names
  private val stryker4sMvnDeploy = "stryker4sMvnDeploy"
  private val publishM2 = "stryker4s-core/publishM2"
  private val crossPublish = "+publish"
  private val crossPublishSigned = "+publishSigned"

  lazy val releaseCommands: Setting[Seq[Command]] = commands ++= Seq(
    // Called by sbt-ci-release
    Command.command(stryker4sPublish)(crossPublish :: publishM2 :: stryker4sMvnDeploy :: _),
    Command.command(stryker4sPublishSigned)(crossPublishSigned :: publishM2 :: stryker4sMvnDeploy :: _),
    // Called by stryker4sPublish(signed)
    Command.command(stryker4sMvnDeploy)(mvnDeploy(baseDirectory.value, version.value))
  )

  /** Sets version of mvn project, calls `mvn deploy` and fails state if the command fails
    */
  private def mvnDeploy(baseDir: File, version: String)(state: State): State =
    mvnGoal(s"versions:set -DnewVersion=$version", baseDir) #&&
      mvnGoal(s"deploy --settings settings.xml -DskipTests", baseDir) #&&
      // Reset version setting after deployment
      mvnGoal("versions:revert", baseDir) ! match {
      case 0 => state
      case _ => state.fail
    }

  /** Returns a `ProcessBuilder` that runs the given maven command in the maven subdirectory
    */
  private def mvnGoal(command: String, baseDir: File): process.ProcessBuilder =
    process.Process(s"mvn --batch-mode $command -P release", baseDir / "runners" / "maven")

}
