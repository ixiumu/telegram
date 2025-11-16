@file:Suppress("UnstableApiUsage")
plugins {
    `kotlin-dsl`
}

val java = JavaVersion.VERSION_17

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()

    dependencies {
        //noinspection UseTomlInstead
        implementation("org.eclipse.jgit:org.eclipse.jgit:7.3.0.202506031305-r")
    }
}

java {
    targetCompatibility = java
    sourceCompatibility = java
}

kotlin {
    jvmToolchain(java.toString().toInt())
}
