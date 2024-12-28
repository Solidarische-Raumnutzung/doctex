plugins {
    kotlin("jvm") version "2.1.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

group = "de.mr_pine"
version = "1.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("fr.inria.gforge.spoon:spoon-javadoc:11.1.0")
    implementation("fr.inria.gforge.spoon:spoon-core:11.1.0")
    implementation(project(":annotations"))

    implementation("com.github.ajalt.clikt:clikt:4.2.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

val mainClassName = "de.mr_pine.doctex.CliKt"
application {
    mainClass = mainClassName
}

tasks {
    withType<Jar> {
        manifest {
            attributes["Main-Class"] = mainClassName
        }
    }

    val moveArtifacts by creating(Copy::class) {
        from(shadowJar)
        into("pages")
        rename { "doctex.jar" }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Solidarische-Raumnutzung/DocTeX")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}