# Stryker4s Maven plugin

This is the project for the Stryker4s Maven plugin. As you cannot build a Maven plugin from a sbt project, it is a separate project.

## Developing

The maven plugin depends on the `stryker4s-core` dependency. To install it locally, you can execute the following command in the root of this repository: `sbt 'set version in ThisBuild := "SET-BY-SBT-SNAPSHOT"' stryker4s-core/publishM2`. This will install `stryker4s-core` into your local Maven repository so you can start local development.
