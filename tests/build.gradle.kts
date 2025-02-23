plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

group = "com.klee0kai.crossbox.tests"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

ksp {
    logger.isEnabled(LogLevel.DEBUG)
}

dependencies {
    implementation(project(":core"))
    ksp(project(":processor"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
}