import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") version "2.3.0"
    `java-library`
    `maven-publish`
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()
description = providers.gradleProperty("pluginDescription").get()

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.lavalink.dev/releases")
    maven("https://maven.lavalink.dev/snapshots")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("dev.arbjerg.lavalink:plugin-api:${providers.gradleProperty("pluginApiVersion").get()}")
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
            artifactId = providers.gradleProperty("pluginArtifactId").get()
            from(project.components["java"])

            pom {
                name.set("Bilibili Plugin")
                description.set("Bilibili source plugin for Lavalink")
                url.set("https://github.com/kyokusakin/bilibili-source")

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("kyokusakin")
                        name.set("kyokusakin")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/kyokusakin/bilibili-source.git")
                    developerConnection.set("scm:git:ssh://git@github.com/kyokusakin/bilibili-source.git")
                    url.set("https://github.com/kyokusakin/bilibili-source")
                }
            }
        }
    }

    repositories {
        val mavenUsername = findProperty("MAVEN_USERNAME") as String?
        val mavenPassword = findProperty("MAVEN_PASSWORD") as String?
        if (!mavenUsername.isNullOrBlank() && !mavenPassword.isNullOrBlank()) {
            val targetRepo = if (version.toString().endsWith("-SNAPSHOT")) {
                "https://maven.lavalink.dev/snapshots"
            } else {
                "https://maven.lavalink.dev/releases"
            }

            maven(targetRepo) {
                credentials {
                    username = mavenUsername
                    password = mavenPassword
                }
            }
        }
    }
}
