// Top-level build file for StreamFlix - Android Streaming App
// Liquid Glass Design with Extension System

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.5" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.5")
    }
}

// Version constants for the entire project
object Versions {
    const val COMPILE_SDK = 34
    const val MIN_SDK = 21
    const val TARGET_SDK = 34
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0-LiquidGlass"
}
