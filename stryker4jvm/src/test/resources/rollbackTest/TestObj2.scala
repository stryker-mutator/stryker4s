package stryker4s.mutants

object TestObj2 {
  // spacing to get different mutant on exact same line as in TestObj1

  def str(a: String): Boolean = {
    a == "blah"
  }
}
