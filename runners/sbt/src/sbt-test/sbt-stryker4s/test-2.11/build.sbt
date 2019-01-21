scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

scalacOptions := Seq("-deprecation", "-Xfatal-warnings")