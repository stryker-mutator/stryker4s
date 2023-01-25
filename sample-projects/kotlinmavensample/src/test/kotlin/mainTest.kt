// import org.scalatest.funspec.AnyFunSpec
// import org.scalatest.matchers.should.Matchers

// class MainTest extends AnyFunSpec with Matchers {
//     describe("Test1") {
//         it("Should return is positive on a positive number") {
//             val m = new Main()
//             m.isPositive(1) shouldBe "Is positive!"
//         }

//         it("Should return is negative on a negative number") {
//             val m = new Main()
//             m.isPositive(-1) shouldBe "Is negative!"
//         }
//     }
// }

package kotlinmavensample

import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {
    @Test
    fun test1() {
        val m = Main();
        assertEquals("Is positive!", m.isPositive(1))
    }

    @Test
    fun test2() {
        val m = Main();
        assertEquals("Is negative!", m.isPositive(-1))
    }

}