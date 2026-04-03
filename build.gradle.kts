import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") version "2.3.0" apply false
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()
description = providers.gradleProperty("projectDescription").get()

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
