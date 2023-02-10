---
sidebar_label: Contributing
title: Contribute to Stryker4s
custom_edit_url: https://github.com/stryker-mutator/stryker4s/edit/master/docs/contributing.md
sidebar_position: 3
---

## Developing on stryker4jvm:
Currently, stryker4jvm is using 3 build tools for all its modules. Stryker4jvm-core is managed by maven,
stryker4jvm-mutator-kotlin is managed by gradle and all other modules are managed by sbt. Below you will find the 
dependency diagram of stryker4jvm's modules.

![Alt text](module_dependencies-stryker4jvm.png?raw=true "Module dependency diagram for stryker4jvm")

In order to build/test stryker4jvm locally you need to take the following steps:

Note: stryker4jvm-mutator-kotlin only works with java versions that are 15 or less due to illegal reflections. This
limitation was transferred from the original stryker4k project.

1) Installing stryker4jvm-core locally:
    1) cd to stryker4jvm-core root
    2) Perform a clean install: `mvn clean install`.
2) Installing stryker4jvm-mutator-kotlin locally:
    1) Verify version of gradle daemon by running `gradlew --version` in the root of mutator kotlin module.
    
       In order to change the JVM version that gradle uses, either change your JAVA_HOME environment variable or specify the version to use
       for all gradle commands like so: `gradlew <command> -Dorg.gradle.java.home=<path-to-jdk>`.
       
    2) If there were version issues or gradle build errors you should delete the .gradle folder and build folder. Note that
       if you changed your JAVA_HOME you may need to restart your computer. Additionally, make sure that no instance of
       a gradle daemon is running with the wrong java version. This can be done by terminating all daemons: `gradlew --stop`.
    3) `gradlew clean build`
    4) `gradlew publishToMavenLocal`
3) Building all other sbt modules:
    1) cd to project root (should contain build.sbt file)
    2) Compilation: `sbt compile` (if you wish to only compile a specific module, use `sbt <module-name>/compile`)
    3) Testing: `sbt test`

 This is the contribution guide for Stryker4s. Great to have you here! Here are a few ways you can help to make this project better.
## Getting started

To get started with developing Stryker4s, you'll need a couple of tools:

- [Java JDK](https://openjdk.java.net/), a recent version like 11 or 17 is recommended
- [sbt](https://www.scala-sbt.org/), to build and test the project

Once these tools are installed you can open the project with [IntelliJ](https://www.jetbrains.com/idea/), or [VS Code](https://code.visualstudio.com/) combined with [Metals](https://scalameta.org/metals/).

If you use VS Code with Metals, you can also install the [Bloop CLI](https://scalacenter.github.io/bloop/) for easier compiling and testing via the command-line.

If you are having issues with setup, or want to keep a clean environment you can also use the [VS Code Remote Containers](https://code.visualstudio.com/docs/remote/containers) feature to develop in a clean reproducible Docker container. All you need for it is VS Code, the [Remote Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers) and [Docker](https://www.docker.com/). Then run 'Remote-Containers: Open Repository in Container...' and enter `stryker-mutator/stryker4s` as the repository. The devcontainer also has the [Bloop](https://scalacenter.github.io/bloop/) and [Coursier](https://get-coursier.io/) CLI tools installed.

## Adding a new feature

New features are welcome! Both as ideas or in the form of a pull request.

1. Please [create an issue](https://github.com/stryker-mutator/stryker4s/issues/new) with your idea first or let us know via [Gitter](https://gitter.im/stryker-mutator/stryker4s).
2. Create a fork on your GitHub account.
3. When writing your code, please conform the existing coding style. We use Scalafmt as a code formatter. You can format your code by running `./bin/scalafmt`, or with editor-specific settings. It also helps to take a moment to review the [Scala style guide](https://docs.scala-lang.org/style/).
4. Please create or edit unit/integration tests for any changed or added code.
5. Confirm everything still works by running `sbt test` (or let the CI do the work for you).
6. Submit the pull request!

Don't hesitate or get discouraged to get in touch! We are always happy to help you if you get stuck or have a question. Even if you don't finish something it can still be a good contribution.

## Running Stryker4s on Stryker4s

We support mutation testing Stryker4s with Stryker4s! The easiest way is to follow our guide in the root readme. If you want to test any local changes, follow these steps:

1. Run `sbt publishPluginLocal` to publish a test snapshot as `0.0.0-TEST-SNAPSHOT` version to your local ivy repository.
2. Add the sbt plugin to `project/plugins.sbt` with `0.0.0-TEST-SNAPSHOT` as the version number.
   1. If the project you are running against is not a 2.13 project, publish `stryker4s-api` and `stryker4s-sbt-testrunner` manually for your appropriate version (.e.g. `sbt "stryker4s-api2_11/publishLocal; stryker4s-sbt-testrunner2_11/publishLocal"`)
3. Run stryker4s as described in the readme.

## Learning resources

Here are some resources you can use if you are new to mutation testing:

- [Scala Love in the City 2021 - Hugo van Rijswijk - Who is Testing yout Tests?](https://youtu.be/Vq9eqZzblfg)
- [What is mutation testing?](https://stryker-mutator.io/) (and the rest of the website). On the Stryker mutator website.
- [Mutation Testing: Complete Guide - Guru99](https://www.guru99.com/mutation-testing.html)
- [Scala Days 2019 - Daniel Westheide - Testing in the postapocalyptic future](https://portal.klewel.com/watch/webcast/scala-days-2019/talk/18/)
- [TechDays 2017 - Simon de Lang - Using Mutation Testing to Improve your JavaScript Tests](https://youtu.be/ba_86FlRiKg)

## Mutation switching

Stryker4s uses a technique called 'mutation switching' to perform mutations. It does this by adding all mutations into a single pattern match, and activating the correct mutation via an environment variable. This would change the following code:

```scala
def isAdult(person: Person) = {
  person.age >= 18
}
```

To:

```scala
def isAdult(person: Person) = {
  sys.env.get("ACTIVE_MUTATION") match {
    case Some("1") => person.age > 18
    case Some("2") => person.age < 18
    case Some("3") => person.age == 18
    case _         => person.age >= 18 // Original
  }
}
```

The effect is the same as compiling each mutation separately, but instead we only have to do it once. This is a big performance improvement, but does mean we have to be more careful about compile errors. Read more about mutation switching on [our blog](https://stryker-mutator.io/blog/2018-10-6/mutation-switching)

## Community

Want to help in some other ways? Great! Here are some things you could do:

- Evangelize mutation testing
  - Mutation testing is still relatively new, especially in Scala. Please help us get the word out there!
  - Share your stories in blog posts and on social media. And please let us know about it!
- Did you use Stryker4s? Your feedback is very valuable to us. Both good and bad! Please [contact us](https://gitter.im/stryker-mutator/stryker4s) to let us know what you think.
