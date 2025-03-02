plugins {
    alias(libs.plugins.jvm)

    alias(libs.plugins.publish.maven)
    alias(libs.plugins.publish.crossbox)
}

group = "com.github.klee0kai.crossbox.core"
version = libs.versions.crossbox.get()

dependencies {
    implementation(libs.bundles.kotlin)
}