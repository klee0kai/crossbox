plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.publish.maven)
    alias(libs.plugins.publish.crossbox)
}

group = "com.github.klee0kai.crossbox.processor"
version = libs.versions.crossbox.get()

dependencies {
    implementation(project(":core"))

    implementation(libs.bundles.kotlin)
    implementation(libs.bundles.kotlinpoet)
    implementation(libs.ksp)

}