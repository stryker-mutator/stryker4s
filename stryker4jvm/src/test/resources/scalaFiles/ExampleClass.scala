package stryker4s

class ExampleClass {
  def foo(num: Int) = num == 10

  def createHugo = Person(22, "Hugo")
}

final case class Person(age: Int, name: String)
