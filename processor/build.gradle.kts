plugins {
    kotlin("jvm")
}

group = "com.klee0kai.crossbox.processor"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":core"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.squareup:kotlinpoet:2.0.0")
    implementation("com.squareup:kotlinpoet-ksp:2.0.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.10-1.0.30")
}