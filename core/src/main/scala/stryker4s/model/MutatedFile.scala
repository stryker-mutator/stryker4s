package stryker4s.model

import better.files.File

import scala.meta.Tree

case class MutatedFile(fileOrigin: File,
                       tree: Tree,
                       mutants: Seq[RegisteredMutant])
