plugins {
    // We declare plugins without application. We avoid possible version conflicts.
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}


// Disable yarn lock file validation
rootProject.tasks.matching { it.name == "kotlinStoreYarnLock" }.all {
    enabled = false
}