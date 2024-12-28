plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "annotations"
            from(components["java"])
        }
    }
}

tasks {
    val moveArtifacts by creating(Copy::class) {
        from(jar)
        into(rootProject.projectDir.resolve("pages"))
        rename { "annotations.jar" }
    }
}