plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    signing
}

description = providers.gradleProperty("pluginDescription").get()

dependencies {
    implementation(project(":common"))
    compileOnly("dev.arbjerg.lavalink:plugin-api:${providers.gradleProperty("pluginApiVersion").get()}")
}

base {
    archivesName.set(providers.gradleProperty("pluginArtifactId").get())
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
            from(components["java"])
            artifact(tasks.named("javadocJar"))

            pom {
                name.set("Bilibili Plugin")
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
        val centralUsername = findProperty("centralPortalUsername") as String?
        val centralPassword = findProperty("centralPortalPassword") as String?
        if (!centralUsername.isNullOrBlank() && !centralPassword.isNullOrBlank()) {
            val targetRepo = if (version.toString().endsWith("-SNAPSHOT")) {
                providers.gradleProperty("centralSnapshotsUrl").get()
            } else {
                providers.gradleProperty("centralReleasesUrl").get()
            }

            maven(targetRepo) {
                credentials {
                    username = centralUsername
                    password = centralPassword
                }
            }
        }
    }
}

val signingKey = findProperty("signingInMemoryKey") as String?
val signingPassword = findProperty("signingPassword") as String?

if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
    signing {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
