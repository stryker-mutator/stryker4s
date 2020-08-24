[![Mutation testing badge](https://img.shields.io/endpoint?style=flat&url=https%3A%2F%2Fbadge-api.stryker-mutator.io%2Fgithub.com%2Fstryker-mutator%2Fstryker4s%2Fmaster%3Fmodule%3Dmaven-plugin)](https://dashboard.stryker-mutator.io/reports/github.com/stryker-mutator/stryker4s/master?module=maven-plugin)

# Stryker4s Maven plugin

This is the project for the Stryker4s Maven plugin. As you cannot build a Maven plugin from a sbt project, it is a separate project.

## Overriding compiler flags

Stryker4s can generate mutated code that produces warnings.
This will not work well with `-Xfatal-warnings` and/or `-Werror` Scala compiler options.
To mitigate this, you can specify a profile where the scala maven plugin uses different set of compiler flags.

```xml
    <profile>
        <id>stryker4s</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>net.alchim31.maven</groupId>
                    <artifactId>scala-maven-plugin</artifactId>
                    <configuration>
                        <args combine.children="override">
                            <arg>-language:postfixOps</arg>
                            <arg>-language:higherKinds</arg>
                            <!-- <arg>-Xfatal-warnings</arg> disabling for stryker4s -->
                        </args>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
```

Then you can activate this profile in `stryker4s-maven-plugin`'s `configuration` section. Or activate a profile from the command-line `mvn stryker4s:run -P stryker4s`.

```xml
    <plugin>
        <groupId>io.stryker-mutator</groupId>
        <artifactId>stryker4s-maven-plugin</artifactId>
        <configuration>
            <project>
                <activeProfiles>
                    <profile>
                        <id>stryker4s</id>
                    </profile>
                </activeProfiles>
            </project>
        </configuration>
    </plugin>
```

## Developing

The maven plugin depends on the `stryker4s-core` dependency. To install it locally, you can execute the following command in the root of this repository: `sbt 'set version in ThisBuild := "SET-BY-SBT-SNAPSHOT"' stryker4s-core2_12/publishM2`. This will install `stryker4s-core_2.12` into your local Maven repository so you can start local development.
