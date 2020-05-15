package stryker4s.config

import stryker4s.testutil.Stryker4sSuite

class TestFilterTest extends Stryker4sSuite {
  describe("filter with default") {
    implicit val conf: Config = Config.default
    it("should work") {
      val testFilter = new TestFilter
      assert(testFilter.filter("stryker4s.extension.FileExtensionsTest"))
    }
  }

  describe("filter with custom") {
    implicit val conf: Config = Config.default.copy(testFilter = Seq("stryker4s.extension.*"))
    it("should work") {
      val testFilter = new TestFilter
      assert(testFilter.filter("stryker4s.extension.FileExtensionsTest"))
      assert(!testFilter.filter("stryker4s.config.TestFilterTest"))
    }
  }

  describe("filter with custom and negation") {
    implicit val conf: Config = Config.default.copy(testFilter = Seq("!stryker4s.extension.*"))
    it("should work") {
      val testFilter = new TestFilter
      assert(!testFilter.filter("stryker4s.extension.FileExtensionsTest"))
      assert(testFilter.filter("stryker4s.config.TestFilterTest"))
    }
  }

}
