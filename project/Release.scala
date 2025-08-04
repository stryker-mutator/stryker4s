import sbt.*
import sbt.Keys.*

import scala.sys.process

object Release {
  // Main release commands
  private val stryker4sPublishSigned = "stryker4sPublishSigned"
  private val stryker4sReleaseAll = "stryker4sReleaseAll"
  // Helper command names
  private val stryker4sMvnDeploy = "stryker4sMvnDeploy"
  private val publishM2Core = "core/publishM2"
  private val crossPublishSigned = "publishSigned"

  lazy val releaseCommands: Setting[Seq[Command]] = commands ++= Seq(
    // Called by sbt-ci-release
    Command.command(stryker4sPublishSigned)(
      crossPublishSigned :: publishM2Core :: stryker4sMvnDeploy :: _
    ),
    // Called by stryker4sPublish(signed)
    Command.command(stryker4sMvnDeploy)(
      mvnDeploy(baseDirectory.value, version.value, (Global / sonaDeploymentName).value)
    )
  )

  /** Sets version of mvn project, calls `mvn deploy` and fails state if the command fails
    */
  private def mvnDeploy(baseDir: File, version: String, sonaDeploymentName: String)(state: State): State = {

    /** Returns a `ProcessBuilder` that runs the given maven command in the maven subdirectory
      */
    def runGoal(command: String): process.ProcessBuilder =
      process.Process(s"mvn --batch-mode --no-transfer-progress $command -P release", baseDir / "maven")

    (runGoal(s"versions:set -DnewVersion=$version") #&&
      runGoal(s"deploy --settings settings.xml -DskipTests -DdeploymentName=$sonaDeploymentName") #&&
      // Reset version setting after deployment
      runGoal("versions:revert")).! match {
      case 0 => state
      case _ => state.fail
    }
  }
}
