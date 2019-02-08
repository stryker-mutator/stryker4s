import sbt.Keys._
import sbt.{Command, Def, _}

import scala.sys.process

object Release {

  // Command names
  val stryker4sMvnPackage = "stryker4sMvnPackage"
  val stryker4sMvnDeploy = "stryker4sMvnDeploy"
  val stryker4sPublish = "stryker4sPublish"
  val stryker4sPublishSigned = "stryker4sPublishSigned"
  val `+publish` = "+publish"
  val `+publishSigned` = "+publishSigned"

  def releaseCommands: Setting[Seq[Command]] = commands ++= Seq(
    Command.command(stryker4sMvnPackage)(mvnPackage(version.value, baseDirectory.value)),
    Command.command(stryker4sMvnDeploy)(mvnDeploy(baseDirectory.value)),
    Command.command(stryker4sPublish)(stryker4sMvnPackage :: `+publish` :: stryker4sMvnDeploy :: _),
    Command.command(stryker4sPublishSigned)(stryker4sMvnPackage :: `+publishSigned` :: stryker4sMvnDeploy :: _),
  )

  /** First publish `stryker4s-core` to local '''maven''' repository,
    * then set new version and run `mvn install`
    */
  private def mvnPackage(version: String, baseDir: File): State => State = state => {
    "stryker4s-core/publishM2" ::
    mapState(
      state,
      runMvn(s"versions:set -DnewVersion=$version", baseDir) #&&
        runMvn("package", baseDir)
    )
  }

  /** Deploy the maven plugin project
    */
  private def mvnDeploy(baseDir: File): State => State = mapState(_, runMvn(s"deploy --settings settings.xml", baseDir))

  /** Runs a process, and fails the state if the process exits with non-zero exit code
    */
  private def mapState(state: State, toRun: process.ProcessBuilder): State = toRun ! match {
    case 0 => state
    case _ => state.fail
  }

  /** Returns a `ProcessBuilder` that runs the given command in the maven subdirectory
    *
    * @param command: The command to run in the maven project
    */
  private def runMvn(command: String, baseDir: File): process.ProcessBuilder =
    process.Process(s"mvn $command -P release", baseDir / "runners" / "maven")

}
