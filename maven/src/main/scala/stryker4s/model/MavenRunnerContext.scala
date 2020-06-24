package stryker4s.model

import better.files.File
import java.{util => ju}

final case class MavenRunnerContext(properties: ju.Properties, goals: Seq[String], tmpDir: File)
    extends TestRunnerContext
