---
title: Configuration
custom_edit_url: https://github.com/stryker-mutator/stryker4s/edit/master/docs/configuration.md
sidebar_position: 2
---

Stryker4s can be configured in multiple ways. You can use a configuration file, set it in your build tool settings, or pass it as command-line arguments. This page describes all configuration options and how to set them.

Configuration methods can be combined seamlessly. The priority is as follows:

1. Command-line arguments
2. Build tool settings
3. Configuration file

| Source         | [CLI args](#cli-args) | [Build tool settings](#build-tool-settings-sbt) | [Config file](#config-file) |
| -------------- | --------------------- | ----------------------------------------------- | --------------------------- |
| Sbt plugin     | ✅                    | ✅                                              | ✅                          |
| Maven plugin   | ❌                    | ❌                                              | ✅                          |
| Command runner | ✅                    | N/A                                             | ✅                          |

<small>Build tool and CLI configuration is available since Stryker4s v0.17.0. If you are looking to configure previous versions, see [here](https://github.com/stryker-mutator/stryker4s/blob/v0.16.1/docs/configuration.md).</small>

### CLI args

All configuration options can be set from the command line. The command line arguments are the same as the config keys, but in kebab-case. For example, `base-dir` becomes `--base-dir`. The command line arguments are case-insensitive.

```sh
sbt stryker --base-dir submodule1 --mutate "**/main/scala/**/*.scala"
```

:::tip
Some options also have short versions. You can use the `--help` flag to see all available options, their short and long forms, and their descriptions.
:::

### Build tool settings (sbt)

Some default values will be read by the sbt project configuration, but you can override sbt setting keys. Each key is named `stryker` followed by the camelCase name of the config key:

```scala
strykerBaseDir := "submodule1"
strykerMutate := Seq("src/main/scala/**/*.scala")
core / strykerDashboardModule := "core"
```

You can also see the value of a key by running e.g. `sbt show strykerBaseDir`.

:::tip
In `*.sbt` files the keys are imported automatically, in `project/*.scala` files you can import them from `import stryker4s.sbt.Stryker4sPlugin.autoImport._`. Your editor will autocomplete the keys for you.
:::

### Config file

The `stryker4s.conf` file is read from the root of the project. This file is read in the HOCON-format. All configuration should be in the "stryker4s" namespace and in kebab-case.

```conf
stryker4s {
  base-dir = "submodule1"
  mutate = [ "**/main/scala/**/*.scala" ]
  # Your configuration here
}
```

## General config

### `mutate` (`Seq[String]`)

**Config file:** `mutate: [ "**/main/scala/**/*.scala" ]`  
**Sbt:** `strykerMutate := Seq("**/main/scala/**/*.scala")`  
**CLI:** `--mutate "**/main/scala/**/*.scala"`  
**Default value:** `[ "**/main/scala/**/*.scala" ]`

With `mutate` you configure the subset of files to use for mutation testing.
Generally speaking, these should be your own source files.  
The default for this will find files in the common Scala project format.

You can _ignore_ files by adding an exclamation mark (`!`) at the start of an expression.

### `test-filter` (`Seq[String]`)

**Config file:** `test-filter: [ "com.mypackage.MyTest" ]`  
**Sbt:** `strykerTestFilter := Seq("com.mypackage.MyTest")`  
**CLI:** `--test-filter com.mypackage.MyTest`  
**Default value:** `[]`  
**Since:** `v0.8.0`

With `test-filter` you configure the subset of tests to use for mutation testing. By default, all tests are included.
You can use a wildcard pattern: `com.mypackage.*`.

- With sbt [`Tests.Filter`](https://www.scala-sbt.org/1.x/docs/Testing.html#Filter+classes) is used.
  - You can _ignore_ tests by adding an exclamation mark (`!`) at the start of an expression.
- With Maven and the ScalaTest plugin, [`wildcardSuites`](https://www.scalatest.org/user_guide/using_the_scalatest_maven_plugin) property is used
- With Maven and the SureFire plugin, [`tests`](https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html) property is used

Note: Not supported in the command-runner plugin.

### `files` (`Seq[String]`)

**Config file:** `files: [ "**/main/scala/**/*.scala" ]`  
**Sbt:** `strykerFiles := Seq("**/main/scala/**/*.scala")`  
**CLI:** `--files "**/main/scala/**/*.scala"`  
**Default value:** `[ "**", "!target/**", "!project/**", "!.metals/**", "!.bloop/**", "!.idea/**" ]`

With `files` you can choose which files should be included in your mutation run sandbox.
This is normally not needed as it defaults to all files not ignored by git. If you do need to override `files` (for example, when your project isn't in a git repository), you can override the `files` config.

You can _ignore_ files by adding an exclamation mark (`!`) at the start of an expression.

### `base-dir` (`String`)

**Config file:** `base-dir: '/usr/your/project/folder/here'`  
**Sbt:** `strykerBaseDir := file("./submodule1")`  
**CLI:** `--base-dir submodule1`  
**Default value:** The current working directory

With `base-dir` you specify the directory from which stryker4s starts and searches for mutations. The default for this is the directory from which the project is being run, which should be fine in most cases. This value can also be relative to the current working directory, E.G.: `base-dir: submodule1` to set the base-dir to a submodule of your project.

### `reporters` (`Seq[String]`)

**Config file:** `reporters: ["console", "html", "json", "dashboard"]`  
**Sbt:** `strykerReporters := Seq("console", "html", "json", "dashboard")`  
**CLI:** `--reporters html --reporters console`  
**Default value:** The `console` and `html` reporters

With `reporters` you can specify reporters for stryker4s to use. The following reporters are supported:

- `console` will output progress and the final result to the console.
- `html` outputs a nice HTML report to `target/stryker4s-report-$timestamp/index.html`. See the [mutation-testing-elements repo](https://github.com/stryker-mutator/mutation-testing-elements/tree/master/packages/elements#readme) for more information.
- `json` writes a json of the mutation result to the same folder as the HTML reporter. The JSON is in the [mutation-testing-report-schema](https://github.com/stryker-mutator/mutation-testing-elements/tree/master/packages/report-schema#readme) format.
- `dashboard` reporter sends a report to https://dashboard.stryker-mutator.io, enabling you to add a fancy mutation score badge to your readme, as well as hosting your HTML report on the dashboard! It uses the [dashboard.\*](#dashboard-object) configuration options. See the [dashboard docs](../General/dashboard.md) for more info. The dashboard reporter only works on JDK 11 or higher.

### `open-report` (`Boolean`)

**Config file:** `open-report: true`
**Sbt:** `strykerOpenReport := true`
**CLI:** `--open-report`
**Default value:** `false`

Open the HTML report in the default browser after the mutation run is finished.

### `excluded-mutations` (`Seq[String]`)

**Config file:** `excluded-mutations: ["BooleanLiteral"]`  
**Sbt:** `strykerExcludedMutations := Seq("BooleanLiteral")`  
**CLI:** `--excluded-mutations BooleanLiteral`  
**Default value:** `[]`

With `excluded-mutations`, you can turn off certain mutations in the project. Allowed values are the following:

- `EqualityOperator`
- `BooleanLiteral`
- `ConditionalExpression`
- `LogicalOperator`
- `StringLiteral`
- `MethodExpression`

### `thresholds` (`object`)

**Config file:** `thresholds{ high=80, low=60, break=0 }`  
**Sbt:** `strykerThresholdsHigh := 80; strykerThresholdsLow := 60; strykerThresholdsBreak := 0`  
**CLI:** `--thresholds.high 80 --thresholds.low 60 --thresholds.break 0`  
**Default values:** `high=80`, `low=60`, `break=0`

Specify the thresholds for mutation scores.

- `mutation score >= high`: Success! Mutation score will be logged at the INFO level.
- `high > mutation score >= low`: Warning! Mutation score will be logged at the WARN level.
- `mutation score < low`: Dangerously low! Mutation score will be logged at the ERROR level with an extra warning.
- `mutation score < break`: Error! Stryker will exit with exit code 1, indicating a build failure.

Setting `break=0` (default value) ensures that the build will never fail.

### `dashboard.*` (`object`)

**Config file:** `dashboard { module="core" }`  
**Sbt:** `dashboardProject := "myproject"; dashboardModule := "core"; `  
**CLI:** `--dashboard.project "myproject --dashboard.module core`  
**Default values:** `dashboard { base-url="https://dashboard.stryker-mutator.io", project="github.com/$USER/$PROJECT_NAME", report-type=full, version=$BRANCH }` if filled by CI environment

Settings for the dashboard [reporter](#reporters-seqstring). See the [dashboard docs](../General/dashboard.md). Note that the values should be kebab-case, not camelCase. If nothing is configured, Stryker4s will try to retrieve the values from one of the supported CI environments:

- Travis
- CircleCI
- GitHub actions

### `scala-dialect` (`String`)

**Config file:** `scala-dialect: "2.13"`  
**Sbt:** `strykerScalaDialect := scala.meta.dialects.Scala3`  
**CLI:** `--scala-dialect "scala3"`  
**Default value:** `scala213source3`  
**Since:** `v0.10.1`

Set the Scala dialect that should be used for parsing Scala files. The default is Scala 2.13 with `-XSource:3` as this has the widest compatibility. If you are running into issues with parsing older unsupported Scala syntax that we forgot about you can change this value.

Valid values are Scala-versions without a patch version (`scala2.12`, `212`, `2.12`, `2`, `3`, `3.2`). If you use `-Xsource:3` you can use `scala212source3` or `scala213source3`. The full list can be found [here](https://github.com/stryker-mutator/stryker4s/blob/master/modules/core/src/main/scala/stryker4s/config/codec/CirisConfigDecoders.scala#L109-L121).

### `static-tmp-dir` (`Boolean`)

**Config file:** `static-tmp-dir: true`  
**Sbt:** `strykerStaticTmpDir := true`  
**CLI:** `--static-tmp-dir`  
**Default value:** `false`  
**Since:** `v0.15.0`

This will force the temporary dir to be created at a static path (`target/stryker4s-tmpDir`).

Recommended when using stryker4s with Bazel (Bazel uses the path to the workspace to determine where to store the
local cache - hashes the path to create the sandbox's name) as it speeds up mutation testing greatly.

### `clean-tmp-dir` (`Boolean`)

**Config file:** `clean-tmp-dir: false`  
**Sbt:** `strykerCleanTmpDir := false`  
**CLI:** `--clean-tmp-dir=false`  
**Default value:** `true`  
**Since:** `v0.15.0`

Temporary dir will be autodeleted at exit if this option is set. Turning it off is useful for debugging purposes.
If cleaning the temporary dir is disabled, you need to clean the temporary dir manually before restarting stryker4s.

On error the temporary dir is never deleted (even if this option is set).

## Sbt plugin config

### `timeout-factor` (`Double`)

**Config file:** `timeout-factor: 1.5`  
**Sbt:** `strykerTimeoutFactor := 1.5`  
**CLI:** `--timeout-factor 1.5`  
**Default value:** `1.5`  
**Since:** `v0.10.0`

See [timeout](#timeout-finiteduration)

### `timeout` (`FiniteDuration`)

**Config file:** `timeout: 5000`  
**Sbt:** `strykerTimeout := 5.seconds`  
**CLI:** `--timeout 5s`  
**Default value:** `5 seconds`  
**Since:** `v0.10.0`

When Stryker4s is mutating code, it cannot determine indefinitely whether a code mutation results in an infinite loop (see [Halting problem](https://en.wikipedia.org/wiki/Halting_problem)).
In order to battle infinite loops, a test run gets killed after a certain period of time. This period is configurable with two settings: `timeout` and `timeoutFactor`.
To calculate the actual timeout in milliseconds the following formula is used:

```
timeoutForTestRun = netTime * timeoutFactor + timeout
```

`netTime` is calculated during the initial test run. The result is logged on `info` level. For example: `Timeout set to 5600ms (net 400 milliseconds)`

With `timeout-factor` you can configure the allowed deviation relative to the time of a normal test run. Tweak this if you notice that mutants are prone to creating slower code, but not infinite loops.
`timeout` lets you configure an absolute deviation. Use it if you run Stryker on a busy machine and you need to wait longer to make sure that the code indeed entered an infinite loop. It can be configured using a number of milliseconds (`5000`) or a duration string (`5s`, `5000ms`)

### `max-test-runner-reuse` (`Int`)

**Config file:** `max-test-runner-reuse: 5`  
**Sbt:** `strykerMaxTestRunnerReuse := 3`  
**CLI:** `--max-test-runner-reuse 3`  
**Default value:** disabled  
**Since:** `v0.10.0`

Restart the testrunner child process after every `n` mutation runs. Not recommended unless you are experiencing memory leaks that you are unable to resolve.

### `legacy-test-runner` (`Boolean`)

**Config file:** `legacy-test-runner: true`  
**Sbt:** `strykerLegacyTestRunner := true`  
**CLI:** `--legacy-test-runner`  
**Default value:** `false`  
**Since:** `v0.10.0`

Use the sbt testrunner that was the default before `v0.10.0`. This testrunner is a lot slower, so it is recommended to only enable this if you are running into issues with the new testrunner. You might want to take a look at [`max-test-runner-reuse`](#max-test-runner-reuse-int) first.

Cases where you might want to use this:

- Your code has lots of 'static' mutants (e.g. `val` in an `object`) which the new testrunner can not test.
- You are running into a bug with the new testrunner.
- Your testframework does not work with the testrunner.

For the last two cases, please [let us know by creating an issue](https://github.com/stryker-mutator/stryker4s/issues/new)!

### `concurrency` (`Int`)

**Config file:** `concurrency: 4`  
**Sbt:** `strykerConcurrency := 4`  
**CLI:** `--concurrency 4`  
**Default value:** `(cpuCoreCount / 4).rounded + 1`  
**Since:** `v0.12.0`

Set the concurrency of testrunners. Stryker4s will create this many testrunners to run mutants in parallel. This defaults to `(cpuCoreCount / 4).rounded + 1`. `cpuCoreCount` includes virtual processors such as from hyperthreading. This is a sane default for most use cases as most test frameworks already have some form of concurrency built in. But as always with concurrency, test it yourself to be sure of the best performance.

### `debug` (`object`)

Describes the `debug` config field

#### `debug-test-runner` (`Boolean`)

**Config file:** `debug { debug-test-runner: true }`  
**Sbt:** `strykerDebugDebugTestRunner := true`  
**CLI:** `--debug-test-runner`  
**Default value:** `false`  
**Since:** `v0.14.0`

Passes additional JVM options to the created testrunner which debuggers can use to attach and debug. Debugging is opened on port 8000. Also limits concurrency to 1. How to debug is specific to your IDE. The used JVM debug argument is:

```
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000
```

To debug in VS Code, you can use (and edit) this `launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "scala",
      "name": "Attach to sbt test-runner",
      "request": "attach",
      "hostName": "127.0.0.1",
      "port": 8000,
      "buildTarget": "sbtTestRunner"
    }
  ]
}
```

#### `log-test-runner-stdout` (`Boolean`)

**Config file:** `debug { log-test-runner-stdout: true }`  
**Sbt:** `strykerDebugLogTestRunnerStdout := true`  
**CLI:** `--log-test-runner-stdout`  
**Default value:** `false`  
**Since:** `v0.14.0`

By default, stdout from testrunners is not logged. With this option, stdout is sent to debug logging. Enabling this can be useful when debugging the testrunners, but is disabled by default because the output can be too much for normal debug logging.

## Process runner config

### `test-runner` (`object`)

**Config file:** `test-runner: { command: "sbt", args: "test" }`  
**Sbt:** N/A  
**CLI:** `--test-runner-command "sbt" --test-runner-args "test"`  
**Mandatory:** Yes

With `test-runner` you specify how stryker4s can invoke the test runner.  
Examples would be `sbt test`, `mvn test` or any other command to run your tests, including any parameters your tests might need.

**warning** The process runner should only be used when your specific test framework is not supported. Due to performance and predictability reasons.

## Other configuration options

### `log-level` (`String`)

**Default value:** `INFO`

How to adjust the loglevel depends on how you run stryker4s:

- sbt plugin
  - Add `stryker / logLevel := Level.Debug` to your build.sbt. Or use `set stryker / logLevel := Level.Debug` if you are in a sbt session.
  - Options: `Debug`, `Info`, `Warn`, `Error`
- Commandrunner
  - Pass the loglevel as a parameter when running, like so: `--debug`
  - Options: `--debug`, `--info`, `--warn`, `--error` (not case sensitive)
- Maven plugin
  - As a command-line property, like so: `mvn -Dorg.slf4j.simpleLogger.defaultLogLevel=warn stryker4s:run`
    - Options: `trace`, `debug`, `info`, `warn`, or `error`
  - Debug logging with `-X` or `-debug`: `mvn -debug stryker4s:run`

**warning** This option cannot be set from stryker4s.conf.

## Excluding specific mutations

**Since:** `v0.9.0`

Using the `@SuppressWarnings` annotation, you can tell Stryker4s to ignore mutations in a code block. Annotate a code block with a `@SuppressWarnings` annotation, passing an array of mutation names you would like to ignore.

```scala
/** No booleans will be mutated
 */
@SuppressWarnings(Array("stryker4s.mutation.BooleanLiteral"))
RequestLogger(logHeaders = false, logBody = false)(ResponseLogger(logHeaders = false, logBody = true)(httpClient))
```

All mutation names are the same as for [Excluded mutations](#excluded-mutations-seqstring) and should be prefixed with `"stryker4s.mutation."`.
