package stryker4s.model

//This class is used to contain information about mutants that did not compile
//It essentially exists so that we don't have to pass around the SBT specific compiler exception
case class CompileError(msg: String, path: String, line: Integer) {
  override def toString: String = s"$path:L$line: '$msg'"
}
