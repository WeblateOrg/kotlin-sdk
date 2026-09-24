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

        project.plugins.withType(AppPlugin::class.java) {
            val androidComponents = project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                // Default configuration for extension
                extension.metadataFile.convention(
                    project.layout.buildDirectory.file(
                        "outputs/weblate/${variant.name}/metadata.json"
                    )
                )

                // Metadata generation task
                val generationTaskProvider = project.tasks.register(
                    "generateMetadataForWeblate${variant.name.replaceFirstChar { it.uppercase() }}",
                    GenerateMetadataTask::class.java
                ) { task ->
                    task.group = Constants.WEBLATE_TASK_GROUP
                    task.description = GenerateMetadataTask.TASK_DESCRIPTION
                    task.packageName.set(variant.applicationId)
                    task.versionCode.set(variant.outputs.first().versionCode.map { it.toLong() })
                    task.outputFile.set(extension.metadataFile)
                }

                variant.artifacts.use(generationTaskProvider)
                    .wiredWith(GenerateMetadataTask::rFile)
                    .toListenTo(SingleArtifact.RUNTIME_SYMBOL_LIST)

                // Metadata upload task
                project.tasks.register(
                    "uploadMetadataForWeblate${variant.name.replaceFirstChar { it.uppercase() }}",
                    UploadMetadataTask::class.java
                ) { task ->
                    task.group = Constants.WEBLATE_TASK_GROUP
                    task.description = UploadMetadataTask.TASK_DESCRIPTION
                    task.dependsOn(generationTaskProvider)
                    task.authToken.set(extension.authToken)
                    task.apiUrl.set("${extension.serverUrl.get()}/api/components/${extension.project.get()}/${extension.component.get()}/addons/kotlin-sdk/builds/")
                    task.metadataFile.set(extension.metadataFile)
                }
            }
        }
    }
}
