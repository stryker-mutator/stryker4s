package stryker4s.api.testprocess

import scala.util.Try

// Messages serialized between the main Stryker4s process and its subprocess where tests are run.
// Each case class has a `@SerialVersionUID` so serializing in-between Scala versions work

sealed trait Message

sealed trait Request extends Message

@SerialVersionUID(3008503622726292148L)
final case class SetupTestContext(context: TestProcessContext) extends Request

@SerialVersionUID(4929497926875736311L)
final case class StartTestRun(mutation: Int) extends Request
@SerialVersionUID(6539766406312948278L)
final case class StartInitialTestRun() extends Request

sealed trait Response extends Message
@SerialVersionUID(549618399043999164L)
final case class SetupTestContextSuccesful() extends Response

sealed trait TestResultResponse extends Response
@SerialVersionUID(7287069995681357334L)
final case class TestsSuccessful() extends TestResultResponse
@SerialVersionUID(2877149475182945995L)
final case class TestsUnsuccessful() extends TestResultResponse

@SerialVersionUID(5801266848315151179L)
final case class TestProcessConfig(
    port: Int
) {
  def toArgs: Seq[String] =
    Seq(
      s"--port=$port"
    )
}

object TestProcessConfig {
  def fromArgs(args: Seq[String]): Option[TestProcessConfig] = {
    val mappedArgs = args.toSeq
      .filter(_.startsWith("--"))
      .map(_.drop(2))
      .map(_.split('='))
      .filter(_.length == 2)
      .map(arr => (arr(0), arr(1)))
      .toMap
    for {
      port <- mappedArgs.get("port").flatMap(s => Try(s.toInt).toOption)
    } yield TestProcessConfig(port)
  }
}
