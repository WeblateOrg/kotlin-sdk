/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.plugin.android

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin for android applications that introduces some useful tasks to localize with Weblate
 */
public class WeblateAndroidPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "weblate", WeblateAndroidPluginExtension::class.java
        )
        // Default server URL for API calls
        extension.serverUrl.convention("https://hosted.weblate.org")

        project.plugins.withType(AppPlugin::class.java) {
            val androidComponents = project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                val outputPath = "outputs/weblate/${variant.name}/metadata.json"
                val taskProvider = project.tasks.register(
                    "generateWeblateJsonConfigFor${variant.name.replaceFirstChar { it.uppercase() }}",
                    GenerateJsonTask::class.java
                ) { task ->
                    task.group = "weblate"
                    task.description = "Generates JSON metadata for Weblate"
                    task.packageName.set(variant.applicationId)
                    task.versionCode.set(variant.outputs.first().versionCode.map { it.toLong() })
                    task.outputFile.set(project.layout.buildDirectory.file(outputPath))
                }

                project.tasks.register(
                    "uploadWeblateJsonConfigFor${variant.name.replaceFirstChar { it.uppercase() }}",
                    UploadJsonTask::class.java
                ) { task ->
                    task.group = "weblate"
                    task.description = "Uploads generated JSON metadata to Weblate"
                    task.authToken.set(extension.authToken)
                    task.apiUrl.set("${extension.serverUrl.get()}/api/components/${extension.project.get()}/${extension.component.get()}/addons/kotlin-sdk/builds/")
                    task.metadataFile.set(project.layout.buildDirectory.file(outputPath))
                }

                variant.artifacts.use(taskProvider)
                    .wiredWith(GenerateJsonTask::rFile)
                    .toListenTo(SingleArtifact.RUNTIME_SYMBOL_LIST)
            }
        }
    }
}
