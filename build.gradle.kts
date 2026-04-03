import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") version "2.3.0"
    `java-library`
    `maven-publish`
}

group = "dev.lavalink.bilibili"
version = "1.0.0-SNAPSHOT"
description = "Bilibili source plugin for Lavalink"

repositories {
    mavenCentral()
    maven("https://maven.lavalink.dev/releases")
    maven("https://maven.lavalink.dev/snapshots")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("dev.arbjerg.lavalink:plugin-api:3.6.1")
}

base {
    archivesName.set("bilibili-plugin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("lavalink-plugins/*.properties") {
        expand("version" to project.version)
    }
}

publishing {
    publications {
        create<MavenPublication>("plugin") {
            artifactId = "bilibili-plugin"
            from(project.components["java"])

            pom {
                name.set("Bilibili Plugin")
                description.set("Bilibili source plugin for Lavalink")
                url.set("https://github.com/your-org/lavalink-bilibili")

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}
