group = "io.stryker-mutator"
version = "0.1.0"

plugins {
    kotlin("jvm") version "1.5.10" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

nexusPublishing {
    repositories {
        sonatype { }
    }
}