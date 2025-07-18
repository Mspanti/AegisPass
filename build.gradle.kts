// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
buildscript {
    repositories {
        google() // This is CRUCIAL for AndroidX libraries
        mavenCentral()
    }
    dependencies {
        // Your existing classpath dependencies like Android Gradle Plugin
        classpath("com.android.tools.build:gradle:8.1.4") // Or whatever version you have
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0") // Or your Kotlin version
    }
}
