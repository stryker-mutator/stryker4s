package example

import java.nio.file.{Files, Paths}

object TestObj {
  def test2(a: String): Boolean = {
    Files.exists(Paths.get(a)) //Should not get mutated!
  }

  def least(a: Int, b: Int): Int = {
    (a, b) match {
      case (a, b) if a < b  => a
      case (a, b) if a > b  => b
      case (a, b) if a == b => 0
    }
  }
}
