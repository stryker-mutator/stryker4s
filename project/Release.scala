import sbt.Keys._
import sbt.{Command, _}

import scala.sys.process

object Release {

  // Main release commands
  private val stryker4sPublish = "stryker4sPublish"
  private val stryker4sPublishSigned = "stryker4sPublishSigned"
  // Helper command names
  private val stryker4sMvnSetup = "stryker4sMvnSetup"
  private val stryker4sMvnPackage = "stryker4sMvnPackage"
  private val stryker4sMvnDeploy = "stryker4sMvnDeploy"
  private val publishM2 = "stryker4s-core/publishM2"
  private val `+publish` = "+publish"
  private val `+publishSigned` = "+publishSigned"

  lazy val releaseCommands: Setting[Seq[Command]] = {
    commands ++= {
      val versionNumber = version.value // Remember the version because sbt-dynver will try to set another one after changes in runner/maven/pom.xml
      Seq(
        // Called by sbt-ci-release
        Command.command(stryker4sPublish)(stryker4sMvnSetup :: `+publish` :: stryker4sMvnDeploy :: _),
        Command.command(stryker4sPublishSigned)(stryker4sMvnSetup :: `+publishSigned` :: stryker4sMvnDeploy :: _),
        // Called by stryker4sPublish(signed)
        Command.command(stryker4sMvnSetup)(publishM2 :: stryker4sMvnPackage :: setVersion(versionNumber) :: _),
        Command.command(stryker4sMvnPackage)(mvnPackage(versionNumber, baseDirectory.value)),
        Command.command(stryker4sMvnDeploy)(mvnDeploy(baseDirectory.value))
      )
    }
  }

  /** First publish `stryker4s-core` to local '''maven''' repository,
    * then set new version and run `mvn install`
    */
  private def mvnPackage(version: String, baseDir: File): State => State = handleProcessResult(
    _,
    mvnGoal(s"versions:set -DnewVersion=$version", baseDir) #&&
      mvnGoal("package", baseDir)
  )

  /** Deploy the maven plugin project
    */
  private def mvnDeploy(baseDir: File): State => State = handleProcessResult(
    _,
    mvnGoal(s"deploy --settings settings.xml", baseDir)
  )

  /** Runs a process, and fails the state if the process exits with non-zero exit code
    */
  private def handleProcessResult(state: State, toRun: process.ProcessBuilder): State = toRun ! match {
    case 0 => state
    case _ => state.fail
  }

  /** Returns a `ProcessBuilder` that runs the given command in the maven subdirectory
    *
    * @param command: The command to run in the maven project
    */
  private def mvnGoal(command: String, baseDir: File): process.ProcessBuilder =
    process.Process(s"mvn --batch-mode $command -P release", baseDir / "runners" / "maven")

  /** After setting the version in the sbt project, there will be local git changes.
    * this causes `sbt-dynver` to add the timestamp to the end of the version.
    * To get around this, the version is remembered from the start, and set again after any maven goals
    */
  private def setVersion(newVersion: String): String = s"""set version in ThisBuild := "$newVersion""""
}
