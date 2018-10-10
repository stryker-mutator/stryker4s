import xerial.sbt.Sonatype._

sonatypeProfileName := "io.stryker-muator"
publishMavenStyle := true
licenses := Seq("Apache-2.0" -> url("https://github.com/stryker-mutator/stryker4s/blob/master/LICENSE"))

sonatypeProjectHosting := Some(GitHubHosting("strykermutator-npa", "stryker4s", "strykermutator-npa@github.com"))

homepage := Some(url("https://stryker-mutator.io/"))
scmInfo := Some(
  ScmInfo(url("https://github.com/stryker-mutator/stryker4s"),
          "git@github.com:stryker-mutator/stryker4s.git"))
developers := List(
  Developer("legopiraat", "Legopiraat", "", url("https://github.com/legopiraat")),
  Developer("hugo-vrijswijk", "Hugo", "@hugo_ijslijk", url("https://github.com/hugo-vrijswijk"))
)

publishTo := sonatypePublishTo.value