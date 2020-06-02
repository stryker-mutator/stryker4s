package stryker4s.model

import better.files.File
import sbt.Def
import sbt.Extracted
import sbt.State

final case class SbtRunnerContext(settings: Seq[Def.Setting[_]], extracted: Extracted, newState: State, tmpDir: File)
    extends TestRunnerContext
