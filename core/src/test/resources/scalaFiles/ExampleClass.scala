package stryker4s

class ExampleClass {
  def foo(num: Int) = num == 10

  def createHugo = Person(22, "Hugo")
}

case class Person(age: Int, name: String)
