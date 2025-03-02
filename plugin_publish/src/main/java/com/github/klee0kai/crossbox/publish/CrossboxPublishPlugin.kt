package com.github.klee0kai.crossbox.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.plugins.PublishingPlugin

/**
 * apply MavenPublishPlugin with configs
 */
class CrossboxPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply(PublishingPlugin::class.java)

        project.afterEvaluate {
            project.extensions.configure(PublishingExtension::class.java) {
                crossboxToMaven(project)
            }
        }
    }
}