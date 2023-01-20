import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MainTest extends AnyFunSpec with Matchers {
    describe("Test1") {
        it("Should return is positive on a positive number") {
            val m = new Main()
            m.isPositive(1) shouldBe "Is positive!"
        }

        it("Should return is negative on a negative number") {
            val m = new Main()
            m.isPositive(-1) shouldBe "Is negative!"
        }
    }
}