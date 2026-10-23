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
import org.weblate.plugin.android.extension.WeblateAndroidPluginExtension
import org.weblate.plugin.android.task.GenerateConfigTask
import org.weblate.plugin.android.task.GenerateMetadataTask
import org.weblate.plugin.android.task.UploadMetadataTask

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
            val apiUrl = extension.serverUrl
                .zip(extension.project) { server, project -> server to project }
                .zip(extension.component) { (server, project), component ->
                    "$server/api/components/$project/$component/addons/kotlin-sdk/builds/"
                }

            extension.sdk.metadataDir.convention(
                project.layout.buildDirectory.dir("outputs/weblate/metadata/")
            )

            androidComponents.onVariants { variant ->
                val variantName = variant.name.replaceFirstChar { it.uppercase() }

                // Metadata generation task
                val metadataTaskProvider = project.tasks.register(
                    "generateMetadataForWeblate$variantName",
                    GenerateMetadataTask::class.java
                ) { task ->
                    task.group = Constants.WEBLATE_TASK_GROUP
                    task.description = GenerateMetadataTask.TASK_DESCRIPTION
                    task.packageName.set(variant.applicationId)
                    task.versionCodes.set(variant.outputs.map { output ->
                        output.versionCode.get()
                    })
                    task.outputDir.set(extension.sdk.metadataDir.dir(variant.name))
                }

                variant.artifacts.use(metadataTaskProvider)
                    .wiredWith(GenerateMetadataTask::rFile)
                    .toListenTo(SingleArtifact.RUNTIME_SYMBOL_LIST)

                // Metadata upload task
                project.tasks.register(
                    "uploadMetadataForWeblate$variantName",
                    UploadMetadataTask::class.java
                ) { task ->
                    task.group = Constants.WEBLATE_TASK_GROUP
                    task.description = UploadMetadataTask.TASK_DESCRIPTION
                    task.dependsOn(metadataTaskProvider)
                    task.authToken.set(extension.authToken)
                    task.apiUrl.set(apiUrl)
                    task.metadataDir.set(extension.sdk.metadataDir.dir(variant.name))
                    task.versionCodes.set(variant.outputs.map { output ->
                        output.versionCode.get()
                    })
                }

                // Config generation task
                if (extension.sdk.cdnUrl.isPresent) {
                    val configTaskProvider = project.tasks.register(
                        "generateConfigForWeblate$variantName",
                        GenerateConfigTask::class.java
                    ) { task ->
                        task.cdnUrl.set(extension.sdk.cdnUrl)
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
}
