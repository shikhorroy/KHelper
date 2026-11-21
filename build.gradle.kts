import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.10.5"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "com.roy"
// The release workflow passes the version from the Git tag, for example -PpluginVersion=2.0.0 for tag v2.0.0.
version = providers.gradleProperty("pluginVersion").getOrElse("2.0.0-SNAPSHOT")

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
            <h3>New</h3>
            <ul>
              <li>Action icons in the KHelper tool window title bar: open source, open in browser, copy submission,
                  delete and settings.</li>
              <li>Duplicate a test case with one click. Test toolbar order: Add, Edit, Duplicate, Delete.</li>
              <li>Clear verdicts for each test: Accepted, Wrong Answer, Runtime Error, Time Limit Exceeded.</li>
              <li>Settings show the server port, with its default and a Reset to Default link.</li>
              <li>New icons for the New UI, and a new KHelper logo.</li>
            </ul>
            <h3>Changed</h3>
            <ul>
              <li>Main is generated after every run, whether the tests pass or not.</li>
              <li>Each test starts with fresh static state, a large stack and a time limit, like on online judges.</li>
              <li>Projects with JDK 11 or newer are supported (Java 21 was required before).</li>
              <li>Supports all IntelliJ IDEA 2025.3 builds and newer, including the K2 Kotlin mode.</li>
              <li>Smaller plugin: no bundled Ktor, Netty, coroutines or Kotlin stdlib.</li>
            </ul>
            <h3>Fixed</h3>
            <ul>
              <li>A problem from Competitive Companion is created only in the active project, not in every open one.</li>
              <li>The local server listens on localhost only (IPv4 and IPv6) and stops when the plugin unloads.</li>
              <li>A busy port shows a message instead of breaking the startup.</li>
              <li>Overwriting an existing solution works.</li>
              <li>Main generation works when the solution has helper classes, and for Java helper classes.</li>
              <li>The tool window updates when test cases change, and shows line breaks in test input.</li>
            </ul>
        """.trimIndent()
    }

    // Secrets come from environment variables, set only by the Publish workflow.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        //~ An unlocked private key, so no key password is needed
        privateKey = providers.environmentVariable("PRIVATE_KEY")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
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
