package stryker4s.model

import cats.Show

//This class is used to contain information about mutants that did not compile
//It essentially exists so that we don't have to pass around the SBT specific compiler exception
final case class CompilerErrMsg(msg: String, path: String, line: Integer)

object CompilerErrMsg {
  implicit def showCompilerErrMsg: Show[CompilerErrMsg] = Show.show(e => s"L${e.line}: ${e.msg}")
}
