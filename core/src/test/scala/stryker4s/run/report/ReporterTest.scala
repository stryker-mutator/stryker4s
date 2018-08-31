package stryker4s.run.report
import org.scalatest.BeforeAndAfterEach
import stryker4s.{Stryker4sSuite, TestAppender}
import stryker4s.config.Config
import stryker4s.model.MutantRunResults

import scala.concurrent.duration._
import scala.language.postfixOps

class ReporterTest extends Stryker4sSuite with BeforeAndAfterEach {

  describe("reporter") {
    it("should log that the console reporter is used when a non existing reporter is configured") {
      implicit val conf: Config = Config(reporters = List("foo", "bar"))

      val sut: Reporter = new Reporter()
      val mutantRunResults = MutantRunResults(List.empty, 100.0, 10 seconds)

      sut.report(mutantRunResults)

      "Configured reporter(s) [foo, bar] were not found." shouldBe loggedAsWarning
      "Using console reporter." shouldBe loggedAsWarning
    }
  }

  override def afterEach(): Unit = {
    TestAppender.reset()
  }
}
