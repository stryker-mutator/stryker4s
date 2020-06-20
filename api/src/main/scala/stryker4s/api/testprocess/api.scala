package stryker4s.api.testprocess

import scala.util.Try

sealed trait Message

sealed trait Request extends Message

final case class SetupTestContext(context: TestProcessContext) extends Request

final case class StartTestRun(mutation: Option[Int]) extends Request

sealed trait Response extends Message

final case class SetupTestContextSuccesful() extends Response

sealed trait TestResultResponse extends Response
final case class TestsSuccessful() extends TestResultResponse
final case class TestsUnsuccessful() extends TestResultResponse

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
