plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

group = "com.klee0kai.crossbox.tests"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)

}

ksp {
    logger.isEnabled(LogLevel.DEBUG)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


dependencies {
    implementation(project(":core"))
    ksp(project(":processor"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}