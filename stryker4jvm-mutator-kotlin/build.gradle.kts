group = "io.stryker-mutator"
version = "1.0"

plugins {
    kotlin("jvm") version "1.4.20" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

nexusPublishing {
    repositories {
        sonatype { }
    }
}