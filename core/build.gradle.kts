plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.publish.maven)
    alias(libs.plugins.publish.crossbox)
}

group = "com.github.klee0kai.crossbox.core"
version = libs.versions.crossbox.get()

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
        binaries.executable()
    }
    wasmJs {
        browser()
        nodejs()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.kotlin)
        }
    }
}

