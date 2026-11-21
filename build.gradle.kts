import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.10.5"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "com.roy"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// The IDE provides the Kotlin stdlib and coroutines. Plugins must not bundle them, even as transitive dependencies.
// See https://plugins.jetbrains.com/docs/intellij/using-kotlin.html
configurations.runtimeClasspath {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains", module = "annotations")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.3.1")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)

        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.java")
    }

    // JSON parsing of the Competitive Companion payload. The IDE copy is internal to the platform, so the plugin
    // bundles its own.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    // Instrumentation compiles GUI Designer forms and adds @NotNull checks to Java code. The plugin has no forms,
    // and the only Java class is the test runner, so it is not needed.
    instrumentCode = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
        }

        changeNotes = """
            <ul>
              <li>Problems from Competitive Companion go to the active project only.</li>
              <li>The local server listens on localhost only and stops when the plugin unloads.</li>
              <li>Each test runs with fresh static state, a time limit and a large stack.</li>
              <li>Main is generated only when all tests pass.</li>
              <li>Overwriting an existing solution works.</li>
              <li>Settings page shows the server port and the auto-generate option.</li>
              <li>Smaller plugin: no bundled Ktor, Netty, coroutines or Kotlin stdlib.</li>
            </ul>
        """.trimIndent()
    }

    pluginVerification {
        ides {
            // The full recommended set downloads several IDE builds and takes long, so it is for CI:
            // ./gradlew verifyPlugin -PverifyAllIdes
            // A local run checks only the build the plugin compiles against.
            if (providers.gradleProperty("verifyAllIdes").isPresent) {
                recommended()
            } else {
                create(IntelliJPlatformType.IntellijIdea, "2025.3.1")
            }
        }
    }
}

tasks {
    // The sandbox IDE uses its own server port (sandboxPort in gradle.properties). Real installs use Settings.
    runIde {
        val port = providers.gradleProperty("sandboxPort")
        jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkhelper.server.port=${port.get()}") }
    }

    // The test runner is the only Java source. It runs inside the user's project JDK, not the IDE JDK,
    // so it targets Java 11 to work with older project SDKs.
    withType<JavaCompile> {
        options.release.set(11)
    }
}

kotlin {
    // IntelliJ IDEA 2025.3 runs on Java 21. The toolchain also gives the code instrumentation a full JDK 21.
    jvmToolchain(21)

    compilerOptions {
        // Use real Java default methods and no DefaultImpls bridges. Bridges override every default method of a
        // platform interface, so the plugin verifier reports them as uses of deprecated and experimental API.
        jvmDefault.set(org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY)
    }
}
