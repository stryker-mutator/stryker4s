package stryker4s.mutants
object TestObj2 {
  def str(a: String): Boolean = {
    _root_.scala.sys.props.get("ACTIVE_MUTATION") match {
      case Some("5") =>
        a != "blah"
      case Some("6") =>
        a == ""
      case _ =>
        a == "blah"
    }
  }
}
