[![Build status](https://img.shields.io/travis/stryker-mutator/stryker4s/master.svg)](https://travis-ci.org/stryker-mutator/stryker4s)
[![Gitter](https://badges.gitter.im/stryker-mutator/stryker.svg)](https://gitter.im/stryker-mutator/stryker4s?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![BCH compliance](https://bettercodehub.com/edge/badge/stryker-mutator/stryker4s?branch=master)](https://bettercodehub.com/)

# Stryker4s

*Professor X: For someone who hates mutants... you certainly keep some strange company.*  
*William Stryker: Oh, they serve their purpose... as long as they can be controlled.*

## Introduction

For an introduction to mutation testing and Stryker's features, see [stryker-mutator.io](https://stryker-mutator.io/). Looking for mutation testing in [JavaScript](https://github.com/stryker-mutator/stryker) or [.NET](https://github.com/stryker-mutator/stryker-net)?

## Getting Started

For the quickstart, see [our website's quickstart](https://stryker-mutator.io/stryker4s/quickstart)

Stryker4s is a mutation testing framework for Scala. It allows you to test your tests by temporarily inserting bugs. Depending on your project setup, there are multiple ways to get started with Stryker4s.

## Sbt plugin

Stryker4s provides a sbt plugin for easy use within sbt projects. To install the plugin, add the following line to `plugins.sbt`:

```scala
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.1.0")
```

After adding the plugin, Stryker4s can be used by running `sbt stryker` in the root of your project.

### Memory usage

Mutation testing can be very taxing on your computer's resources. After all, all your tests are ran for each mutant. Sbt also has the tendency to build up memory in a running session. It's wise to give sbt some more memory by setting the following environment variable: `export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=4G -Xmx4G"`. The amount of memory needed is dependent on the project you are running Stryker4s on.

### Multi-module projects

Multi-module projects are not yet fully supported. However, there is a workaround while we work on a better solution. Set the submodule you want to run Stryker4s on before as the active one with `sbt "project $projectname" stryker`.

We do not (yet) have a SBT plugin or other easy ways to run the project. However, the [command test-runner](docs/CONFIGURATION.md#test-runner) should allow you to run the project with any build-tool. The easiest way to test the project right now is by cloning the repository, creating a `stryker4s.conf` file with a `base-dir` of your project and running Stryker4s with `sbt run`.

## Configuration

See [CONFIGURATION.md](docs/CONFIGURATION.md) for setting up your `stryker4s.conf` file.

## Supported mutators

See [MUTATORS.md](docs/MUTATORS.md) for our supported mutators.

## Changelog
See [CHANGELOG.md](CHANGELOG.md) for all the latest change made.

## Contributing

Want to contribute? That's great! Please have a look at our [contributing guide](docs/CONTRIBUTING.md).
