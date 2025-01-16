// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Add JitPack repository for buildscript dependencies
    }
    dependencies {
        // Add dependencies for the Google Services plugin if needed
        classpath("com.google.gms:google-services:4.4.0")

    }
}
