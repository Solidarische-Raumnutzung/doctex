plugins {
    kotlin("jvm") version "1.9.21"
}

group = "de.mr_pine"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("fr.inria.gforge.spoon:spoon-javadoc:10.4.3-beta-13")
    implementation("fr.inria.gforge.spoon:spoon-core:10.4.3-beta-13")

    implementation("com.github.ajalt.clikt:clikt:4.2.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}