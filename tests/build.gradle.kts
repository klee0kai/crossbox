plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ksp)
}

group = "com.klee0kai.crossbox.tests"
version = libs.versions.crossbox.get()

ksp {
    logger.isEnabled(LogLevel.DEBUG)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":core"))
    ksp(project(":processor"))

    implementation(libs.bundles.kotlin)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
}