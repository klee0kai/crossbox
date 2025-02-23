plugins {
//    id("com.google.devtools.ksp")
    kotlin("jvm")
//    kotlin("kapt")
}

group = "com.klee0kai.crossbox.processor"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))

//    implementation("com.github.klee0kai.stone:kotlin_lib:1.0.3")
//    kapt("com.github.klee0kai.stone:stone_processor:1.0.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.10-1.0.30")
}