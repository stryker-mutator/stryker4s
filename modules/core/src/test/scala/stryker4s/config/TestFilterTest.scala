package stryker4s.config

import stryker4s.testkit.Stryker4sSuite

class TestFilterTest extends Stryker4sSuite {
  describe("filter") {

    test("should work with default") {
      implicit val conf: Config = Config.default
      val testFilter = new TestFilter
      assert(testFilter.filter("stryker4s.extension.FileExtensionsTest"))
    }

    test("should work with custom") {
      implicit val conf: Config = Config.default.copy(testFilter = Seq("stryker4s.extension.*"))
      val testFilter = new TestFilter
      assert(testFilter.filter("stryker4s.extension.FileExtensionsTest"))
      assert(!testFilter.filter("stryker4s.config.TestFilterTest"))
    }

    test("should work with custom and negation") {
      implicit val conf: Config = Config.default.copy(testFilter = Seq("!stryker4s.extension.*"))
      val testFilter = new TestFilter
      assert(!testFilter.filter("stryker4s.extension.FileExtensionsTest"))
      assert(testFilter.filter("stryker4s.config.TestFilterTest"))
    }
  }
}
