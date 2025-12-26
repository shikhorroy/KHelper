plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.10.5"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "com.roy"
version = "0.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

val ktorVersion = "3.3.3"
val kotlinxSerializationJson = "1.9.0"

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.3.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.java")
    }

    // Ktor for HTTP server
    implementation("io.ktor:ktor-server-core:$ktorVersion") {
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-server-netty:$ktorVersion") {
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion") {
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion") {
        exclude(group = "org.slf4j")
    }

    // Kotlinx serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJson")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253.29346.138"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
