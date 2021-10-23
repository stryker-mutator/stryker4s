package stryker4s.mutants
import java.nio.file.{Files, Paths}
object TestObj1 {
  def test2(a: String): Boolean = {
    Files.exists(Paths.get(a))
  }
  def least(a: Int, b: Int): Int = {
    _root_.scala.sys.props.get("ACTIVE_MUTATION").map(_root_.java.lang.Integer.parseInt(_)) match {
      case Some(1) =>
        (a, b) match {
          case (a, b) if a <= b => a
          case (a, b) if a == b => 0
        }
      case Some(2) =>
        (a, b) match {
          case (a, b) if a > b  => a
          case (a, b) if a == b => 0
        }
      case Some(3) =>
        (a, b) match {
          case (a, b) if a == b => a
          case (a, b) if a == b => 0
        }
      case Some(4) =>
        (a, b) match {
          case (a, b) if a < b  => a
          case (a, b) if a != b => 0
        }
      case _ =>
        (a, b) match {
          case (a, b) if a < b  => a
          case (a, b) if a == b => 0
        }
    }
  }
}
