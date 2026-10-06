/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
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

            // Default configuration for extension
            extension.metadataDir.convention(
                project.layout.buildDirectory.dir("outputs/weblate/")
            )

            androidComponents.onVariants { variant ->
                // Metadata generation task
                val metadataTaskProvider = project.tasks.register(
                    "generateMetadataForWeblate${variant.name.replaceFirstChar { it.uppercase() }}",
                    GenerateMetadataTask::class.java
                ) { task ->
                    task.group = Constants.WEBLATE_TASK_GROUP
                    task.description = GenerateMetadataTask.TASK_DESCRIPTION
                    task.packageName.set(variant.applicationId)
                    task.versionCodes.set(variant.outputs.map { output ->
                        output.versionCode.get()
                    })
                    task.outputDir.set(extension.metadataDir.dir(variant.name))
                }

                variant.artifacts.use(metadataTaskProvider)
                    .wiredWith(GenerateMetadataTask::rFile)
                    .toListenTo(SingleArtifact.RUNTIME_SYMBOL_LIST)

                // Metadata upload task
                project.tasks.register(
                    "uploadMetadataForWeblate${variant.name.replaceFirstChar { it.uppercase() }}",
                    UploadMetadataTask::class.java
                ) { task ->
                    task.group = Constants.WEBLATE_TASK_GROUP
                    task.description = UploadMetadataTask.TASK_DESCRIPTION
                    task.dependsOn(metadataTaskProvider)
                    task.authToken.set(extension.authToken)
                    task.apiUrl.set("${extension.serverUrl.get()}/api/components/${extension.project.get()}/${extension.component.get()}/addons/kotlin-sdk/builds/")
                    task.metadataDir.set(extension.metadataDir.dir(variant.name))
                    task.versionCodes.set(variant.outputs.map { output ->
                        output.versionCode.get()
                    })
                }

                // Config generation task
                val configTaskProvider = project.tasks.register(
                    "generateConfigForWeblate${variant.name.replaceFirstChar { it.uppercase() }}",
                    GenerateConfigTask::class.java
                ) { task ->
                    task.cdnUrl.set(extension.cdnUrl)
                }

                variant.sources.resources?.addGeneratedSourceDirectory(
                    configTaskProvider,
                    GenerateConfigTask::outputDirectory
                )

                variant.sources.kotlin?.addGeneratedSourceDirectory(
                    configTaskProvider,
                    GenerateConfigTask::outputDirectory
                )
            }
        }
    }
}
