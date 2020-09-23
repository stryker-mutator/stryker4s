# Configuration

All configuration options can be set from the stryker4s.conf file in the root of the project. This file is read in the HOCON-format. All configuration should be in the "stryker4s" namespace and in camel-case.

```conf
stryker4s {
  # Your configuration here
}
```

- [Configuration](#configuration)
  - [General config](#general-config)
    - [mutate](#mutate)
    - [test-filter](#test-filter)
    - [files](#files)
    - [base-dir](#base-dir)
    - [reporters](#reporters)
    - [excluded-mutations](#excluded-mutations)
    - [thresholds](#thresholds)
    - [dashboard.\*](#dashboard)
    - [timeout-factor](#timeout-factor)
    - [timeout](#timeout)
  - [Sbt plugin config](#sbt-plugin-config)
    - [max-test-runner-reuse](#max-test-runner-reuse)
    - [legacy-test-runner](#legacy-test-runner)
  - [Process runner config](#process-runner-config)
    - [test-runner](#test-runner)
  - [Other configuration options](#other-configuration-options)
    - [log-level](#log-level)
    - [Excluding specific mutations](#excluding-specific-mutations)

## General config

#### mutate

**Config file:** `mutate: [ "**/main/scala/**/*.scala" ]`  
**Default value:** `[ "**/main/scala/**/*.scala" ]`  
**Description:**  
With `mutate` you configure the subset of files to use for mutation testing.
Generally speaking, these should be your own source files.  
The default for this will find files in the common Scala project format.

You can _ignore_ files by adding an exclamation mark (`!`) at the start of an expression.

#### test-filter

**Config file:** `test-filter: [ "com.mypackage.MyTest" ]`  
**Default value:** `[]`  
**Since:** `v0.8.0`  
**Description:**  
With `test-filter` you configure the subset of tests to use for mutation testing. By default all tests are included.
You can use wildcard pattern: `com.mypackage.*`.

- With sbt [`Tests.Filter`](https://www.scala-sbt.org/1.x/docs/Testing.html#Filter+classes) is used.
  - You can _ignore_ tests by adding an exclamation mark (`!`) at the start of an expression.
- With Maven and the ScalaTest plugin, [`wildcardSuites`](https://www.scalatest.org/user_guide/using_the_scalatest_maven_plugin) property is used
- With Maven and the SureFire plugin, [`tests`](https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html) property is used

Note: Not supported in the command-runner plugin.

#### files

**Config file:** `files: [ "**/main/scala/**/*.scala" ]`  
**Default value:** result of `git ls-files --others --exclude-standard --cached`  
**Description:**  
With `files` you can choose which files should be included in your mutation run sandbox.
This is normally not needed as it defaults to all files not ignored by git. If you do need to override `files` (for example, when your project isn't in a git repository), you can override the `files` config.

You can _ignore_ files by adding an exclamation mark (`!`) at the start of an expression.

#### base-dir

**Config file:** `base-dir: '/usr/your/project/folder/here'`  
**Default value:** The directory from which the process is started  
**Description:**  
With `base-dir` you specify the directory from which stryker4s starts and searches for mutations. The default for this is the directory from which the project is being run, which should be fine in most cases. This value can also be relative to the current working directory, E.G.: `base-dir: submodule1` to set the base-dir to a submodule of your project.

#### reporters

**Config file:** `reporters: ["console", "html", "json", "dashboard"]`  
**Default value:** The `console` and `html` reporters  
**Description:**  
With `reporters` you can specify reporters for stryker4s to use. The following reporters are supported:

- `console` will output progress and the final result to the console.
- `html` outputs a nice HTML report to `target/stryker4s-report-$timestamp/index.html`. See the [mutation-testing-elements repo](https://github.com/stryker-mutator/mutation-testing-elements/tree/master/packages/mutation-testing-elements#mutation-testing-elements) for more information.
- `json` writes a json of the mutation result to the same folder as the HTML reporter. The JSON is in the [mutation-testing-report-schema](https://github.com/stryker-mutator/mutation-testing-elements/tree/master/packages/mutation-testing-report-schema) format.
- `dashboard` reporter sends a report to https://dashboard.stryker-mutator.io, enabling you to add a fancy mutation score badge to your readme, as well as hosting your HTML report on the dashboard! It uses the [dashboard.\*](#dashboard.*) configuration options. See the [Stryker handbook](https://github.com/stryker-mutator/stryker-handbook/blob/master/dashboard.md) for more info.

#### excluded-mutations

**Config file:** `excluded-mutations: ["BooleanLiteral"]`  
**Default value:** `[]`  
**Description:**  
With `excluded-mutations`, you can turn off certain mutations in the project. Allowed values are the following:

- `EqualityOperator`
- `BooleanLiteral`
- `ConditionalExpression`
- `LogicalOperator`
- `StringLiteral`
- `MethodExpression`

#### thresholds

**Config file:** `thresholds{ high=80, low=60, break=0 }`  
**Default values:** `high=80`, `low=60`, `break=0`  
**Description:**  
Specify the thresholds for mutation scores.

- `mutation score >= high`: Success! Mutation score will be logged at the INFO level.
- `high > mutation score >= low`: Warning! Mutation score will be logged at the WARN level.
- `mutation score < low`: Dangerously low! Mutation score will be logged at the ERROR level with an extra warning.
- `mutation score < break`: Error! Stryker will exit with exit code 1, indicating a build failure.

Setting `break=0` (default value) ensures that the build will never fail.

#### dashboard.\*

**Config file:** `dashboard { module="core" }`  
**Default values:** `dashboard { base-url="https://dashboard.stryker-mutator.io", project="github.com/$USER/$PROJECT_NAME", report-type=full, version=$BRANCH }` if filled by CI environment  
**Description:**  
Settings for the dashboard [reporter](#reporters). See the [stryker handbook for more info](https://github.com/stryker-mutator/stryker-handbook/blob/master/dashboard.md). Note that the values should be kebab-case, not camelCase. If nothing is configured, Stryker4s will try to retrieve the values from one of the supported CI environments:

- Travis
- CircleCI
- GitHub actions

#### timeout-factor

**Config file:** `timeout-factor: 1.5`  
**Default value:** `1.5`  
**Since:** `v0.10.0`  
**Description:**

See [timeout](#timeout)

#### timeout

**Config file:** `timeout: 5000`  
**Default value:** `5 seconds`  
**Since:** `v0.10.0`  
**Description:**

When Stryker4s is mutating code, it cannot determine indefinitely whether a code mutation results in an infinite loop (see [Halting problem](https://en.wikipedia.org/wiki/Halting_problem)).
In order to battle infinite loops, a test run gets killed after a certain period of time. This period is configurable with two settings: `timeout` and `timeoutFactor`.
To calculate the actual timeout in milliseconds the, following formula is used:

```
timeoutForTestRun = netTime * timeoutFactor + timeout
```

`netTime` is calculated during the initial test run. The result is logged on `info` level. For example: `Timeout set to 5600ms (net 400 milliseconds)`

With `timeout-factor` you can configure the allowed deviation relative to the time of a normal test run. Tweak this if you notice that mutants are prone to creating slower code, but not infinite loops.
`timeout` lets you configure an absolute deviation. Use it if you run Stryker on a busy machine and you need to wait longer to make sure that the code indeed entered an infinite loop. It can be configured using a number of milliseconds (`5000`) or a duration string (`5s`, `5000ms`)

## Sbt plugin config

#### max-test-runner-reuse

**Config file:** `max-test-runner-reuse: 5`  
**Default value:** disabled  
**Since:** `v0.10.0`  
**Description:**

Restart the testrunner after every `n` runs. Not recommended unless you are experiencing memory leaks that you are unable to resolve.

#### legacy-test-runner

**Config file:** `legacy-test-runner: true`  
**Default value:** `false`  
**Since:** `v0.10.0`  
**Description:**

Use the sbt testrunner that was the default before `v0.10.0`. This testrunner is a lot slower, so it is recommended to only enable this if you are running into issues with the new testrunner. You might want to take a look at [`max-test-runner-reuse`](#max-test-runner-reuse) first.

Cases where you might want to use this:

- Your code has lots of 'static' mutants (e.g. `val` in an `object`) which the new testrunner can not test.
- You are running into a bug with the new testrunner.
- Your testframework does not work with the testrunner.

For the last two cases, please [let us know by creating an issue](https://github.com/stryker-mutator/stryker4s/issues/new)!

## Process runner config

#### test-runner

**Config file:** `test-runner: { command: "sbt", args: "test" }`  
**Mandatory:** Yes  
**Description:**  
With `test-runner` you specify how stryker4s can invoke the test runner.  
Examples would be `sbt test`, `mvn test` or any other command to run your tests, including any parameters your tests might need.

**warning** The process runner should only be used when your specific test framework is not supported. Due to performance and predictability reasons.

## Other configuration options

#### log-level

**Default value:** `INFO`  
**Description:**  
How to adjust the loglevel depends on how you run stryker4s:

- sbt plugin
  - Add `logLevel in stryker := Level.Debug` to your build.sbt. Or use `set logLevel in stryker := Level.Debug` if you are in a sbt session.
  - Options: `Debug`, `Info`, `Warn`, `Error`
- Commandrunner
  - Pass the loglevel as a parameter when running, like so: `--debug`
  - Options: `--off`, `--error`, `--warn`, `--info`, `--debug`, `--trace`, `--all` (not case sensitive)
- Maven plugin
  - As a command-line property, like so: `mvn -Dorg.slf4j.simpleLogger.defaultLogLevel=warn stryker4s:run`
  - Options: `trace`, `debug`, `info`, `warn`, or `error`

**warning** This option cannot be set from stryker4s.conf.

#### Excluding specific mutations

**Since:** `v0.9.0`

Using the `@SuppressWarnings` annotation, you can tell Stryker4s to ignore mutations in a code block. Annotate a code block with a `@SuppressWarnings` annotation, passing an array of mutation names you would like to ignore.

```scala
/** No booleans will be mutated
 */
@SuppressWarnings(Array("stryker4s.mutation.BooleanLiteral"))
RequestLogger(logHeaders = false, logBody = false)(ResponseLogger(logHeaders = false, logBody = true)(httpClient))
```

All mutation names are the same as for [Excluded mutations](#excluded-mutations) and should be prefixed with `"stryker4s.mutation."`.
