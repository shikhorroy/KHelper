plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    application
}

group = "khelper.cp.roy"
version = "1.0-SNAPSHOT"
val ktorVersion = "3.3.1"
val logbackVersion = "1.5.20"
val serializationJson = "1.8.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationJson")
    runtimeOnly("io.ktor:ktor-server-core:${ktorVersion}")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

tasks.register<JavaExec>("runSolver") {
    group = "application"
    description = "Run the solver in batch mode"
    mainClass.set("RunnerKt")
    classpath = sourceSets["main"].runtimeClasspath
}
