import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    id("org.jetbrains.qodana") version "0.1.12"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("org.jetbrains.changelog") version "1.3.1"
}

version = "1.0.0"
group = "com.github.kotlinisland"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin.jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(9))
}

sourceSets.main.configure {
    resources.include("LICENCE")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "proxyauth.Main"
}

changelog {
    groups.set(listOf("Added"))
}
