import MillScripted.*
import Release.*
import org.typelevel.sbt.tpolecat.*
import org.typelevel.scalacoptions.{ScalaVersion, *}
import sbt.*
import sbt.Keys.*
import sbt.ScriptedPlugin.autoImport.{scriptedBufferLog, scriptedLaunchOpts}
import sbtprotoc.ProtocPlugin.autoImport.PB

import java.io.File.separator
import scala.sys.process.{Process, ProcessLogger}

import TpolecatPlugin.autoImport.*

object MillScripted {

  /** Scripted-style integration test for the Mill plugin. Runs against the sample project(s) under
    * `modules/mill/src/mill-test/`.
    *
    * The required artifacts are published automatically as this task depends on relevant `publishLocal` tasks. Run it
    * with `sbt millPlugin/millScripted`.
    */
  val millScripted = taskKey[Unit]("Run integration tests for the Mill plugin against a real Mill project")

  lazy val millScriptedTask: Def.Initialize[Task[Unit]] = Def.task {
    val log = streams.value.log
    val stryker4sVersion = version.value
    val testProjects = (LocalRootProject / baseDirectory).value / "modules" / "mill" / "src" / "mill-test"
    val workDir = target.value / "mill-scripted"

    IO.delete(workDir)
    IO.createDirectory(workDir)

    val mutate = workDir / "mutate"
    IO.copyDirectory(testProjects / "mutate", mutate)
    // Substitute the locally-published plugin version into the build
    val buildFile = mutate / "build.mill"
    IO.write(buildFile, IO.read(buildFile).replace("@STRYKER4S_VERSION@", stryker4sVersion))
    (mutate / "mill").setExecutable(true)

    def runMill(args: String*): (Int, String) = {
      val output = new StringBuilder
      val logger = ProcessLogger { line =>
        output.append(line).append('\n')
        log.info(s"[mill] $line")
      }
      // `--no-daemon` runs Mill in-process and exits cleanly, so no background daemon leaks in CI
      val command = Seq(s".${separator}mill", "--no-daemon") ++ args
      val os = sys.props("os.name").toLowerCase
      val panderToWindows = os match {
        case n if n.contains("windows") => Seq("cmd", "/C") ++ command
        case _                          => command
      }
      log.info(s"Running: ${panderToWindows.mkString(" ")}")
      val exit = Process(panderToWindows, mutate).!(logger)
      (exit, output.toString())
    }

    log.info(s"Running Mill plugin integration test with stryker4s version $stryker4sVersion")

    // Scenario 1: a normal run should succeed and report a mutation score
    val (exit1, out1) = runMill("foo.stryker")
    assert(exit1 == 0, s"Expected 'foo.stryker' to succeed, but it exited with $exit1")
    assert(out1.contains("Mutation run finished"), "Expected a completed mutation run in the output")
    assert(out1.contains("Mutation score"), "Expected a mutation score in the output")

    // Scenario 2: a break threshold above the (~66%) score, overridden via CLI, should fail the build.
    // `low`/`high` are raised too so they stay above `break` (the config requires high > low > break).
    val breakArgs = Seq("--thresholds.break", "70", "--thresholds.low", "75", "--thresholds.high", "90")
    val (exit2, out2) = runMill(("foo.stryker" +: breakArgs)*)
    assert(exit2 != 0, s"Expected a break threshold above the score to fail the build, but it exited with $exit2")
    assert(
      out2.contains("Mutation score below threshold"),
      "Expected a threshold-break message when the score is below the configured break threshold"
    )

    log.info("Mill plugin integration test passed")
  }
}
