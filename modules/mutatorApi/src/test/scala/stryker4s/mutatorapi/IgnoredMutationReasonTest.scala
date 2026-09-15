package stryker4s.mutatorapi

import stryker4s.testkit.Stryker4sSuite

class IgnoredMutationReasonTest extends Stryker4sSuite {
  test("MutationExcluded should explain it was excluded by user configuration") {
    assertEquals(MutationExcluded.explanation, "Mutation was excluded by user configuration")
  }

  test("RegexParseError should include the pattern and message in its explanation") {
    val reason = RegexParseError("[a-z", "unclosed character class")

    assert(reason.explanation.contains("[a-z"))
    assert(reason.explanation.contains("unclosed character class"))
  }

  test("NoRegexMutationsFound should include the pattern in its explanation") {
    val reason = NoRegexMutationsFound("[a-z]+")

    assert(reason.explanation.contains("[a-z]+"))
  }
}
