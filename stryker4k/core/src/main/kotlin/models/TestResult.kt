package models

class TestResult(val mutation: Mutation, val result: Result)

enum class Result {
    Killed,
    Survived,
    TimedOut
}
