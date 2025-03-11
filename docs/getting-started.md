---
title: Getting started
custom_edit_url: https://github.com/stryker-mutator/stryker4s/edit/master/docs/getting-started.md
sidebar_position: 1
---

Stryker4s is a mutation testing framework for Scala. It allows you to test your tests by temporarily inserting bugs.

This guide is for the sbt plugin for Stryker4s. For other ways to run Stryker4s, such as on Maven projects, look at our [README](https://github.com/stryker-mutator/stryker4s/blob/master/README.md#getting-started).

## 1 Install

To install Stryker4s on your project, add the following line to `project/plugins.sbt` [![Maven Central](https://img.shields.io/maven-central/v/io.stryker-mutator/stryker4s-core_2.12.svg?label=Maven%20Central&colorB=brightgreen)](https://search.maven.org/search?q=g:io.stryker-mutator):

```scala
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % stryker4sVersion)
```

## 2 Configure

Stryker4s can be configured in multiple ways. You can use sbt settings, a configuration file, or command-line arguments. Stryker4s will set defaults for some values, and retrieve project information from your sbt build for others. For more information on how to configure Stryker4s, visit our [configuration page](./configuration.md).

## 3 Let's kill some mutants

Run Stryker4s to mutation test your project:

```shell
sbt stryker
```

## 4 Having trouble?

Are you having troubles? Try enabling debug logging. For more information on how to enable debug logging visit our [configuration page](./configuration.md#log-level-string).
If you are having issues, please let us know by [reporting an issue](https://github.com/stryker-mutator/stryker4s/issues/new) or talk to us on [Slack](https://join.slack.com/t/stryker-mutator/shared_invite/enQtOTUyMTYyNTg1NDQ0LTU4ODNmZDlmN2I3MmEyMTVhYjZlYmJkOThlNTY3NTM1M2QxYmM5YTM3ODQxYmJjY2YyYzllM2RkMmM1NjNjZjM).
