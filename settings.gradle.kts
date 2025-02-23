pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" apply false
    id("com.google.devtools.ksp") version "2.1.10-1.0.30" apply false
    kotlin("jvm") version "2.1.10" apply false
}

rootProject.name = "crossbox"

include("core")
include("processor")
include("tests")

