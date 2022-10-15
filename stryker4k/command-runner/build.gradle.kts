import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.10")
    testImplementation(kotlin("test-junit"))
    testImplementation("io.mockk:mockk:1.10.6")
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

application {
    mainClass.set("MainKt")
    project.setProperty("mainClassName", "MainKt")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "MainKt"
}

tasks.withType<ShadowJar> {
    isZip64 = true
    archiveFileName.set("${project.name}-dependencies.jar")
    //manifest.attributes["Main-Class"] = "MainKt"
}

//tasks.register<Jar>("fatJar") {
//    manifest.attributes["Main-Class"] = "MainKt"
//    from(sourceSets.main.get().output)
//
//    dependsOn(configurations.runtimeClasspath)
//    from({
//        configurations.runtimeClasspath.get()
//            .filter { it.name.endsWith("jar") }
//            .map { zipTree(it) }
//    })
//}

