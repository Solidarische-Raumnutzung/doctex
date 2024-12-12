plugins {
    kotlin("jvm") version "1.9.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.mr_pine"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("fr.inria.gforge.spoon:spoon-javadoc:10.4.3-beta-13")
    implementation("fr.inria.gforge.spoon:spoon-core:10.4.3-beta-13")
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

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}