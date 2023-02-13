[![Maven Central](https://img.shields.io/maven-central/v/io.stryker-mutator/stryker4s-core_2.12.svg?label=Maven%20Central&colorB=brightgreen)](https://search.maven.org/search?q=g:io.stryker-mutator)
[![Build status](https://github.com/stryker-mutator/stryker4s/workflows/CI/badge.svg)](https://github.com/stryker-mutator/stryker4s/actions)
[![Mutation testing badge](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fbadge-api.stryker-mutator.io%2Fgithub.com%2Fstryker-mutator%2Fstryker4s%2Fmaster)](https://dashboard.stryker-mutator.io/reports/github.com/stryker-mutator/stryker4s/master)
[![Slack Chat](https://img.shields.io/badge/slack-chat-brightgreen.svg?logo=slack)](https://join.slack.com/t/stryker-mutator/shared_invite/enQtOTUyMTYyNTg1NDQ0LTU4ODNmZDlmN2I3MmEyMTVhYjZlYmJkOThlNTY3NTM1M2QxYmM5YTM3ODQxYmJjY2YyYzllM2RkMmM1NjNjZjM)

![stryker-80x80](https://user-images.githubusercontent.com/10114577/59962899-d26b8d00-94eb-11e9-8e31-18b3d8d96fd3.png)

# Stryker4jvm

_Professor X: For someone who hates mutants... you certainly keep some strange company._  
_William Stryker: Oh, they serve their purpose... as long as they can be controlled._

## Introduction

For an introduction to mutation testing and Stryker's features, see [stryker-mutator.io](https://stryker-mutator.io/). Looking for mutation testing in [JavaScript](https://github.com/stryker-mutator/stryker) or [.NET](https://github.com/stryker-mutator/stryker-net)?

## Getting Started

For the quickstart, see [our  quickstart](docs/getting-started.md).
See our [sample projects](sample-projects) for examples on how to add stryker to your project.

Stryker4jvm is a mutation testing framework for jvm-languages, currently supporting Scala and Kotlin. It allows you to test your tests by temporarily inserting bugs. 

Depending on your project setup, there are multiple ways to get started with Stryker4jvm.

## Sbt plugin

Stryker4jvm provides a sbt plugin for easy use within sbt projects. To install the plugin, add the following line to `plugins.sbt` [![Maven Central](https://img.shields.io/maven-central/v/io.stryker-mutator/stryker4s-core_2.12.svg?label=Maven%20Central&colorB=brightgreen)](https://search.maven.org/artifact/io.stryker-mutator/sbt-stryker4s):

```scala
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4jvm" % stryker4jvmVersion)
```

After adding the plugin, Stryker4jvm can be used by running `sbt stryker` in the root of your project.

### Multi-module projects

Multi-module projects are not yet fully supported. However, there is a workaround you can use while we work on a better solution. Set the base-directory to the correct directory of the submodule with the [`base-dir` configuration setting](https://stryker-mutator.io/docs/stryker4s/configuration#base-dir-string). Then you can run `sbt "project yourSubmoduleNameHere" stryker` to set the active project and run Stryker.

## Maven plugin

The Maven plugin can be added as follows in `pom.xml` under `<plugins>` 

[comment]: <> (todo: update link)
[comment]: <> ([![Maven Central]&#40;https://img.shields.io/maven-central/v/io.stryker-mutator/stryker4s-core_2.12.svg?label=Maven%20Central&colorB=brightgreen&#41;]&#40;https://search.maven.org/artifact/io.stryker-mutator/stryker4s-maven-plugin&#41;:)

```xml
<plugin>
    <groupId>io.stryker-mutator</groupId>
    <artifactId>stryker4jvm-plugin-maven</artifactId>
    <version>${stryker4jvm.version}</version>
</plugin>
```

You can then run Stryker4jvm with the command `mvn stryker4jvm:run`. Note that this is different than the command for the sbt plugin.

## Pre-release versions

We also publish SNAPSHOT versions of each commit on master. To use a pre-release, add the following setting to your `plugins.sbt`:

```scala
resolvers += Resolver.sonatypeRepo("snapshots")
```

[comment]: <> (todo: update the link from stryker4s-core to stryker4jvm)
[comment]: <> (Then replace the Stryker4s version with this version: [![Sonatype Nexus &#40;Snapshots&#41;]&#40;https://img.shields.io/nexus/s/https/oss.sonatype.org/io.stryker-mutator/stryker4s-core_2.12.svg&#41;]&#40;https://oss.sonatype.org/content/repositories/snapshots/io/stryker-mutator/&#41;.)

## Configuration

See the [configuration page](docs/configuration.md) for setting up your `stryker4jvm.conf` file (optional).

## Supported mutators

Stryker4jvm supports different mutators depending on the language it is being run on. 

For Scala the mutators can be found  [here](https://stryker-mutator.io/docs/mutation-testing-elements/supported-mutators/).
Do you have a suggestion for a (new) mutator? Feel free to create an [issue](https://github.com/stryker-mutator/stryker4s/issues/new)!

An always up-to-date reference is also available in the [MutantMatcher source](stryker4jvm-mutator-scala/src/main/scala/stryker4jvm/mutator/scala/MutantMatcher.scala).

## Changelog

See the [releases page](https://github.com/stryker-mutator/stryker4jvm/releases) for all the latest changes made.

## Contributing

Want to contribute? That's great! Please have a look at our [contributing guide](docs/contributing.md).
