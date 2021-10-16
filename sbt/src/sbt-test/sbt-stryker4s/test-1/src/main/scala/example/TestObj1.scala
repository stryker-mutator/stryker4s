package example

import java.nio.file.{Files, Paths}

object TestObj {

  def mutatesOkay(s: String) = s.exists(_ == 'a')

  def test2(a: String): Boolean = {
    Files.exists(Paths.get(a)) //Should not get mutated!
  }

  def alsoMutatesOkay(s: String) = s.exists(_ == 'b')
}
