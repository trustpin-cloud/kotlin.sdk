pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("android") version "2.3.0"
        kotlin("plugin.compose") version "2.3.0"
        id("com.android.application") version "8.10.1"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TrustPin-Android-Sample"
