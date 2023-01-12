package stryker4jvm.config

import stryker4jvm.testutil.Stryker4jvmSuite

class TestFilterTest extends Stryker4jvmSuite {
  describe("filter") {

    it("should work with default") {
      implicit val conf: Config = Config.default
      val testFilter = new TestFilter
      assert(testFilter.filter("stryker4s.extension.FileExtensionsTest"))
    }

    it("should work with custom") {
      implicit val conf: Config = Config.default.copy(testFilter = Seq("stryker4s.extension.*"))
      val testFilter = new TestFilter
      assert(testFilter.filter("stryker4s.extension.FileExtensionsTest"))
      assert(!testFilter.filter("stryker4s.config.TestFilterTest"))
    }

    it("should work with custom and negation") {
      implicit val conf: Config = Config.default.copy(testFilter = Seq("!stryker4s.extension.*"))
      val testFilter = new TestFilter
      assert(!testFilter.filter("stryker4s.extension.FileExtensionsTest"))
      assert(testFilter.filter("stryker4s.config.TestFilterTest"))
    }
  }
}
