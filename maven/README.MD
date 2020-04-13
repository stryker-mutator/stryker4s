[![Mutation testing badge](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fbadge-api.stryker-mutator.io%2Fgithub.com%2Fstryker-mutator%2Fstryker4s%2Fmaster%3Fmodule%3Dmaven-plugin)](https://dashboard.stryker-mutator.io/reports/github.com/stryker-mutator/stryker4s/master?module=maven-plugin)

# Stryker4s Maven plugin

This is the project for the Stryker4s Maven plugin. As you cannot build a Maven plugin from a sbt project, it is a separate project.

## Developing

The maven plugin depends on the `stryker4s-core` dependency. To install it locally, you can execute the following command in the root of this repository: `sbt 'set version in ThisBuild := "SET-BY-SBT-SNAPSHOT"' stryker4s-coreJVM2_12/publishM2`. This will install `stryker4s-core` into your local Maven repository so you can start local development.
