# Configuration  

All configuration options can be set from the stryker4s.conf file in the root of the project. This file is read in the HOCON-format. All configuration should be in the "stryker4s" namespace.

```conf
stryker4s {
# Your configuration here
}
```

#### Files to mutate

**Config file:** `mutate: [ "**/main/scala/**/*.scala" ]`  
**Default value:** `[ "**/main/scala/**/*.scala" ]`  
**Mandatory:** No  
**Description:**  
With `mutate` you configure the subset of files to use for mutation testing. 
Generally speaking, these should be your own source files.  
The default for this will find files in the common Scala project format. 

You can *ignore* files by adding an exclamation mark (`!`) at the start of an expression.

#### base-dir

**Config file:** `base-dir: '/usr/your/project/folder/here'`  
**Default value:** The directory from which the process is started  
**Mandatory:** No  
**Description:**  
With `base-dir` you specify the directory from which stryker4s starts and searches for mutations. The default for this is the directory from which the project is being run, which should be fine in most cases.

#### test-runner

**Config file:** `test-runner: { type: "commandrunner", command: "sbt", args: "test" }`  
**Default value:** A command-runner with the `sbt test` command  
**Mandatory:** No  
**Description:**  
With `test-runner` you can specify how stryker4s runs tests. The default for this is a command-runner that will run the `sbt test` command. This can be changed to `mvn test`, `./gradlew test` or any other command to run your tests, including any parameters your tests might need.

#### reporters

**Config file:** `reporters: ["console"]`  
**Default value:** A reporter that will report to console.  
**Mandatory:** No  
**Description:**  
With `reporters` you can specify reporters for stryker4s to use. By default the `console` reporter is used which will report to your console.

#### log-level

**Config file:** `log-level: INFO`
**Default value:** INFO
**Mandatory:** No
**Description:**
With `log-level` you can override the default logging level with one of the following [Logback logging levels](https://logback.qos.ch/apidocs/ch/qos/logback/classic/Level.html): `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`, `ALL`, `OFF`.
 