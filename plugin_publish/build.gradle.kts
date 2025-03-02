plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins.register("crossbox-publish") {
        id = "com.github.klee0kai.crossbox.publish"
        implementationClass = "com.github.klee0kai.crossbox.publish.CrossboxPublishPlugin"
    }
}
