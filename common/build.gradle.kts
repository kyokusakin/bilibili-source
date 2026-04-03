plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

description = providers.gradleProperty("commonDescription").get()

dependencies {
    compileOnly("com.github.walkyst.lavaplayer-fork:lavaplayer:${providers.gradleProperty("lavaplayerVersion").get()}")
}

base {
    archivesName.set(providers.gradleProperty("commonArtifactId").get())
}

publishing {
    publications {
        create<MavenPublication>("common") {
            artifactId = providers.gradleProperty("commonArtifactId").get()
            from(components["java"])

            pom {
                name.set("Bilibili Common")
                description.set(project.description)
                url.set(providers.gradleProperty("projectUrl").get())

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set(providers.gradleProperty("developerId").get())
                        name.set(providers.gradleProperty("developerName").get())
                    }
                }

                scm {
                    val projectUrl = providers.gradleProperty("projectUrl").get()
                    connection.set("scm:git:${projectUrl}.git")
                    developerConnection.set("scm:git:ssh://git@github.com/kyokusakin/bilibili-source.git")
                    url.set(projectUrl)
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
