scalaVersion := "2.13.10"
crossScalaVersions := Seq("2.12.17", "2.13.10", "3.2.1")

name := "scala-sbt-sample"
organization := ""
version := "1.0"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1",
  "org.scalatest" %% "scalatest" % "3.2.14" % Test
)
