pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Bump AGP to match Gradle 8.13 compatibility
        id("com.android.application") version "8.12.0"
        // Keep Kotlin aligned with Compose Compiler 1.5.14
        id("org.jetbrains.kotlin.android") version "1.9.24"
        id("org.jetbrains.kotlin.kapt") version "1.9.24"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KronosClock"
include(":app")
