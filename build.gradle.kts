import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") version "2.3.0" apply false
}

fun gitCommand(vararg args: String): String? {
    return runCatching {
        providers.exec {
            commandLine("git", *args)
        }.standardOutput.asText.get().trim()
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}

val releaseTagPrefix = providers.gradleProperty("releaseTagPrefix").get()
val releaseTag = gitCommand("tag", "--points-at", "HEAD", "$releaseTagPrefix*")
    ?.lineSequence()
    ?.map(String::trim)
    ?.firstOrNull()
    ?.removePrefix(releaseTagPrefix)
val snapshotVersion = gitCommand("rev-parse", "--short=12", "HEAD")?.let { "$it-SNAPSHOT" }
    ?: providers.gradleProperty("fallbackSnapshotVersion").get()

group = providers.gradleProperty("pluginGroup").get()
version = releaseTag ?: snapshotVersion
description = providers.gradleProperty("projectDescription").get()

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.lavalink.dev/releases")
        maven("https://maven.lavalink.dev/snapshots")
        maven("https://jitpack.io")
    }
}

subprojects {
    pluginManager.withPlugin("java-library") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
            withSourcesJar()
        }

        tasks.register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")

            // Placeholder documentation jar for Maven Central validation.
            from(rootProject.layout.projectDirectory.file("README.md"))
            from(rootProject.layout.projectDirectory.file("LICENSE"))
        }
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }
}
