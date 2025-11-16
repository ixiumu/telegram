@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.ksp) apply false
    //alias(libs.plugins.rust) apply false
}

tasks.register<Delete>("clean").configure {
    delete(layout.buildDirectory.asFile.get())
}

val apiCode by extra(93)
val verCode = Common.getBuildVersionCode(rootProject)

val verName = if (Version.isStable) {
    "v" + Version.officialVersionName + "-" + (Common.getGitHeadRefsSuffix(rootProject))
} else {
    "v" + Version.officialVersionName + "-preview-" + (Common.getGitHeadRefsSuffix(rootProject))
}

val androidTargetSdkVersion by extra(36)
val androidMinSdkVersion by extra(27)
val androidCompileSdkVersion by extra(36)
val androidBuildToolsVersion by extra("35.0.0")
val androidCompileNdkVersion = "28.2.13676358"

fun Project.configureBaseExtension() {
    extensions.findByType(com.android.build.gradle.BaseExtension::class)?.run {
        compileSdkVersion(androidCompileSdkVersion)
        ndkVersion = androidCompileNdkVersion
        buildToolsVersion = androidBuildToolsVersion

        defaultConfig {
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = verCode
            versionName = verName
        }

        compileOptions {
            sourceCompatibility = Version.java
            targetCompatibility = Version.java
        }

        packagingOptions.jniLibs.useLegacyPackaging = false
    }
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
}
