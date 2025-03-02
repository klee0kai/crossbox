plugins {
    alias(libs.plugins.jvm)
}

group = "com.klee0kai.crossbox.processor"
version = libs.versions.crossbox.get()

dependencies {
    implementation(project(":core"))

    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.kotlinpoet)
    implementation(libs.ksp)

}