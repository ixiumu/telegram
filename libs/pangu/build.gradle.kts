plugins {
    java
    kotlin("jvm")
}

java {
    targetCompatibility = Version.java
    sourceCompatibility = Version.java
}

kotlin {
    jvmToolchain(Version.java.toString().toInt())
}
