pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/klee0kai/maven")
            credentials {
                username = System.getenv("SECRETS_GH_ACTOR")
                    ?: settings.providers.gradleProperty("github.actor").orNull

                password = System.getenv("SECRETS_GH_API_TOKEN")
                    ?: settings.providers.gradleProperty("github.token").orNull
            }
        }
        maven(url = "https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/klee0kai/maven")
            credentials {
                username = System.getenv("SECRETS_GH_ACTOR")
                    ?: settings.providers.gradleProperty("github.actor").orNull

                password = System.getenv("SECRETS_GH_API_TOKEN")
                    ?: settings.providers.gradleProperty("github.token").orNull
            }
        }
        maven(url = "https://jitpack.io")
    }
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

rootProject.name = "crossbox"

includeBuild("plugin_publish")
include("core")
include("processor")
include("tests")

