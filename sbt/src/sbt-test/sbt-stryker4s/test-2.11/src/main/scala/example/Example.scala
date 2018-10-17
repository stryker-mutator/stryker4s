package example

import scala.collection.JavaConversions

object Example {

  def canDrink(age: Int): Boolean = age >= 18

  // This expression doesn't compile with Scala 2.12 + "-deprecation" and "-Xfatal-warnings"
  JavaConversions.asJavaCollection(Nil)

}