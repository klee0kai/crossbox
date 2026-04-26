plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.klee0kai.crossbox.tests"
version = libs.versions.crossbox.get()

ksp {
    logger.isEnabled(LogLevel.DEBUG)
}


kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))

            implementation(libs.bundles.kotlin)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.tablesaw.core)
            implementation(libs.joinery)
            implementation(libs.poi)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(libs.jupiter.api)
            implementation(libs.jupiter.engine)
        }
    }
}

dependencies {
    ksp(project(":processor"))
}