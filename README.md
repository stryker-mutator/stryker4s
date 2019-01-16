[![Build status](https://img.shields.io/travis/stryker-mutator/stryker4s/master.svg)](https://travis-ci.org/stryker-mutator/stryker4s)
[![Gitter](https://badges.gitter.im/stryker-mutator/stryker.svg)](https://gitter.im/stryker-mutator/stryker4s?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![BCH compliance](https://bettercodehub.com/edge/badge/stryker-mutator/stryker4s?branch=master)](https://bettercodehub.com/)
[![Maven Central](https://img.shields.io/maven-central/v/io.stryker-mutator/stryker4s-core_2.12.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:io.stryker-mutator)

# Stryker4s

*Professor X: For someone who hates mutants... you certainly keep some strange company.*  
*William Stryker: Oh, they serve their purpose... as long as they can be controlled.*

**Note: this project is still very much in early development and (probably) not ready to reliably be used in large projects!**

**Despite that, we'd be happy to help you out if you run into any problems or have any questions 😁.**

## Introduction

For an introduction to mutation testing and Stryker's features, see [stryker-mutator.io](https://stryker-mutator.io/). Looking for mutation testing in [JavaScript](https://github.com/stryker-mutator/stryker) or [.NET](https://github.com/stryker-mutator/stryker-net)?

## Getting Started

For the quickstart, see [our website's quickstart](https://stryker-mutator.io/stryker4s/quickstart).

Stryker4s is a mutation testing framework for Scala. It allows you to test your tests by temporarily inserting bugs. Depending on your project setup, there are multiple ways to get started with Stryker4s.

## Sbt plugin

Stryker4s provides a sbt plugin for easy use within sbt projects. To install the plugin, add the following line to `plugins.sbt`:

```scala
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.1.0")
```

After adding the plugin, Stryker4s can be used by running `sbt stryker` in the root of your project.

### Memory usage

Mutation testing can be very taxing on your computer's resources. After all, your tests are run for each mutant. Sbt also has the tendency to [build up memory in a running session](https://github.com/sbt/sbt/issues/3983). It's wise to give sbt some extra memory by setting the following environment variable: `export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=4G -Xmx4G"`. The amount of memory needed depends on the size of the project you are running Stryker4s on.

### Multi-module projects

Multi-module projects are not yet fully supported. However, there is a workaround you can use while we work on a better solution. Set the base-directory to the correct directory of the submodule with the [`base-dir` configuration setting](docs/CONFIGURATION.md#base-dir). Then you can run `sbt "project yourSubmoduleNameHere" stryker` to set the active project and run Stryker4s.

## Configuration

See [CONFIGURATION.md](docs/CONFIGURATION.md) for setting up your `stryker4s.conf` file (optional).

## Supported mutators

See [MUTATORS.md](docs/MUTATORS.md) for our supported mutators.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for all the latest changes made.

## Contributing

Want to contribute? That's great! Please have a look at our [contributing guide](docs/CONTRIBUTING.md).
