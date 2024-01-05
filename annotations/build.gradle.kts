plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "de.mr-pine.doctex"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifactId = "annotations"

                from(components["java"])
            }
        }
        repositories {
            maven {
                name = "reposilite"
                credentials {
                    username = properties["reposilite.user"].toString()
                    password = properties["reposilite.password"].toString()
                }
                url = uri(properties["reposilite.url"].toString())
            }
        }
    }
}

