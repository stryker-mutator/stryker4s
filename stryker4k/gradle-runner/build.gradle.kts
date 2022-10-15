import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.14.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.10")
    testImplementation(kotlin("test-junit"))
    testImplementation("io.mockk:mockk:1.10.6")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.5.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("io.stryker-mutator:mutation-testing-elements:1.7.0")
    implementation("io.stryker-mutator:mutation-testing-metrics-circe_2.13:1.7.0")
    implementation(gradleApi())
}

tasks.test {
    useJUnit()
}

tasks.register<Test>("stryker") {
    outputs.upToDateWhen { false }
    failFast = true
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
    plugins {
        create("stryker4k") {
            id = "io.stryker-mutator.stryker4k"
            displayName = "stryker4k"
            description = "A mutation testing framework for Kotlin."
            implementationClass = "Stryker4kGradlePlugin"
        }
    }
}

pluginBundle {
    website = "https://stryker-mutator.io"
    vcsUrl = "https://github.com/stryker-mutator/stryker4k.git"
    tags = listOf("testing", "unit-testing", "kotlin", "gradle", "mutation-testing", "test-automation", "gradle-plugin", "testing-tools", "stryker", "stryker4k")
}
