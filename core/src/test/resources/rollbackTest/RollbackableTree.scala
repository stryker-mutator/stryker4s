package stryker4s.mutants

import java.nio.file.{Files, Paths}

object Foo {
  def bar(a: String): Boolean = _root_.stryker4s.activeMutation match {
    case 1 => Files.forall(Paths.get(a))
    case _ if _root_.stryker4s.coverage.coverMutant(5) =>
      Files.exists(Paths.get(a))
  }
}
