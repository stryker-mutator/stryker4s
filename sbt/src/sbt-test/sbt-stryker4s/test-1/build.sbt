scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

strykerMutate := Seq("**/main/scala/**/*.scala")