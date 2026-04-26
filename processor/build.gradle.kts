plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.publish.maven)
    alias(libs.plugins.publish.crossbox)
}

group = "com.github.klee0kai.crossbox.processor"
version = libs.versions.crossbox.get()

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":core"))

                implementation(libs.bundles.kotlin)
                implementation(libs.bundles.kotlinpoet)
                implementation(libs.ksp)
                implementation(libs.hummus.ksp)
            }
        }
    }

}
