[![Maven Central](https://img.shields.io/maven-central/v/io.stryker-mutator/stryker4s-core_2.12.svg?label=Maven%20Central&colorB=brightgreen)](https://search.maven.org/search?q=g:io.stryker-mutator)
[![Build status](https://github.com/stryker-mutator/stryker4s/workflows/CI/badge.svg)](https://github.com/stryker-mutator/stryker4s/actions)
[![Mutation testing badge](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fbadge-api.stryker-mutator.io%2Fgithub.com%2Fstryker-mutator%2Fstryker4s%2Fmaster)](https://dashboard.stryker-mutator.io/reports/github.com/stryker-mutator/stryker4s/master)
[![Slack Chat](https://img.shields.io/badge/slack-chat-brightgreen.svg?logo=slack)](https://join.slack.com/t/stryker-mutator/shared_invite/enQtOTUyMTYyNTg1NDQ0LTU4ODNmZDlmN2I3MmEyMTVhYjZlYmJkOThlNTY3NTM1M2QxYmM5YTM3ODQxYmJjY2YyYzllM2RkMmM1NjNjZjM)

![stryker-80x80](https://user-images.githubusercontent.com/10114577/59962899-d26b8d00-94eb-11e9-8e31-18b3d8d96fd3.png)

# Stryker4s

_Professor X: For someone who hates mutants... you certainly keep some strange company._  
_William Stryker: Oh, they serve their purpose... as long as they can be controlled._

## Introduction

For an introduction to mutation testing and Stryker's features, see [stryker-mutator.io](https://stryker-mutator.io/). Looking for mutation testing in [JavaScript](https://github.com/stryker-mutator/stryker) or [.NET](https://github.com/stryker-mutator/stryker-net)?

## Getting Started

For the quickstart, see [our website's quickstart](https://stryker-mutator.io/stryker4s/quickstart).

Stryker4s is a mutation testing framework for Scala. It allows you to test your tests by temporarily inserting bugs. Depending on your project setup, there are multiple ways to get started with Stryker4s.

## Sbt plugin

Stryker4s provides a sbt plugin for easy use within sbt projects. To install the plugin, add the following line to `plugins.sbt` [![Maven Central](https://img.shields.io/maven-central/v/io.stryker-mutator/stryker4s-core_2.12.svg?label=Maven%20Central&colorB=brightgreen)](https://search.maven.org/artifact/io.stryker-mutator/sbt-stryker4s):

```scala
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % stryker4sVersion)
```

After adding the plugin, Stryker4s can be used by running `sbt stryker` in the root of your project.

### Multi-module projects

Multi-module projects are not yet fully supported. However, there is a workaround you can use while we work on a better solution. Set the base-directory to the correct directory of the submodule with the [`base-dir` configuration setting](https://stryker-mutator.io/docs/stryker4s/configuration#base-dir-string). Then you can run `sbt "project yourSubmoduleNameHere" stryker` to set the active project and run Stryker4s.

## Maven plugin

The Maven plugin can be added as follows in `pom.xml` under `<plugins>` [![Maven Central](https://img.shields.io/maven-central/v/io.stryker-mutator/stryker4s-core_2.12.svg?label=Maven%20Central&colorB=brightgreen)](https://search.maven.org/artifact/io.stryker-mutator/stryker4s-maven-plugin):

```xml
<plugin>
    <groupId>io.stryker-mutator</groupId>
    <artifactId>stryker4s-maven-plugin</artifactId>
    <version>${stryker4s.version}</version>
</plugin>
```

You can then run Stryker4s with the command `mvn stryker4s:run`. Note that this is different than the command for the sbt plugin.

## Pre-release versions

We also publish SNAPSHOT versions of each commit on master. To use a pre-release, add the following setting to your `build.sbt` and `plugins.sbt`:

```scala
resolvers ++= Resolver.sonatypeOssRepos("snapshots")
```

Then replace the Stryker4s version with this version: [![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/io.stryker-mutator/stryker4s-core_2.12.svg)](https://oss.sonatype.org/content/repositories/snapshots/io/stryker-mutator/).

## Configuration

See the [configuration page](https://stryker-mutator.io/docs/stryker4s/configuration) for all the configuration options available in Stryker4s.

## Supported mutators

Stryker4s supports a variety of mutators, which can be found [here](https://stryker-mutator.io/docs/mutation-testing-elements/supported-mutators/).
Do you have a suggestion for a (new) mutator? Feel free to create an [issue](https://github.com/stryker-mutator/stryker4s/issues/new)!

An always up-to-date reference is also available in the [MutantMatcher source](core/src/main/scala/stryker4s/mutants/findmutants/MutantMatcher.scala).

## Changelog

See the [releases page](https://github.com/stryker-mutator/stryker4s/releases) for all the latest changes made.

## Contributing

Want to contribute? That's great! Please have a look at our [contributing guide](https://stryker-mutator.io/docs/stryker4s/contributing).
