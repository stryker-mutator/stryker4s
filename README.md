[![Build status](https://img.shields.io/travis/stryker-mutator/stryker4s/master.svg)](https://travis-ci.org/stryker-mutator/stryker4s)
[![Gitter](https://badges.gitter.im/stryker-mutator/stryker.svg)](https://gitter.im/stryker-mutator/stryker4s?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![BCH compliance](https://bettercodehub.com/edge/badge/stryker-mutator/stryker4s?branch=master)](https://bettercodehub.com/)

# Stryker4s

*Professor X: For someone who hates mutants... you certainly keep some strange company.*  
*William Stryker: Oh, they serve their purpose... as long as they can be controlled.*

**Note: this project is still very much in early development and (probably) not ready to reliably be used in large projects!**

**Despite that, we'd be happy to help you out if you run into any problems or have any questions 😁. In the meantime, start by [mutation testing JavaScript with Stryker](https://stryker-mutator.github.io).**

## Introduction
For an introduction to mutation testing and Stryker's features, see [stryker-mutator.io](https://stryker-mutator.io/).

## Getting Started
Stryker4s is a mutation testing framework for Scala. It allows you to test your tests by temporarily inserting bugs.

We do not (yet) have a SBT plugin or other easy ways to run the project. However, the [command test-runner](docs/CONFIGURATION.md#test-runner) should allow you to run the project with any build-tool. The easiest way to test the project right now is by cloning the repository, creating a `stryker4s.conf` file with a `base-dir` of your project and running Stryker4s with `sbt run`.

## Configuration
See [CONFIGURATION.md](docs/CONFIGURATION.md) for setting up your `stryker4s.conf` file.

## Supported mutators
See [MUTATORS.md](docs/MUTATORS.md) for our supported mutators.

## Contributing

Want to contribute? That's great! Please have a look at our [contributing guide](docs/CONTRIBUTING.md).
