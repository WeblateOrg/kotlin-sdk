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
import org.weblate.plugin.android.task.DownloadTranslationFilesTask
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
                    val host = server.removeSuffix("/")
                    "$host/api/components/$project/$component/addons/kotlin-sdk/builds/"
                }

            extension.apply {
                kotlinSdk.metadataDir.convention(
                    project.layout.buildDirectory.dir("outputs/weblate/metadata/")
                )

                translationFiles.outputDir.convention(
                    project.layout.projectDirectory.dir("src/main/res/")
                )
            }

            androidComponents.onVariants { variant ->
                val variantName = variant.name.replaceFirstChar { it.uppercase() }

                // Metadata generation task
                val metadataTaskProvider = project.tasks.register(
                    "generateMetadataForWeblate$variantName",
                    GenerateMetadataTask::class.java
                ) { task ->
                    task.packageName.set(variant.applicationId)
                    task.versionCodes.set(variant.outputs.map { output ->
                        output.versionCode.get()
                    })
                    task.outputDir.set(extension.kotlinSdk.metadataDir.dir(variant.name))
                }

                variant.artifacts.use(metadataTaskProvider)
                    .wiredWith(GenerateMetadataTask::rFile)
                    .toListenTo(SingleArtifact.RUNTIME_SYMBOL_LIST)

                // Metadata upload task
                project.tasks.register(
                    "uploadMetadataForWeblate$variantName",
                    UploadMetadataTask::class.java
                ) { task ->
                    task.dependsOn(metadataTaskProvider)
                    task.authToken.set(extension.authToken)
                    task.apiUrl.set(apiUrl)
                    task.metadataDir.set(extension.kotlinSdk.metadataDir.dir(variant.name))
                    task.versionCodes.set(variant.outputs.map { output ->
                        output.versionCode.get()
                    })
                }

                // Config generation task
                if (extension.kotlinSdk.cdnUrl.isPresent) {
                    val configTaskProvider = project.tasks.register(
                        "generateConfigForWeblate$variantName",
                        GenerateConfigTask::class.java
                    ) { task ->
                        task.cdnUrl.set(extension.kotlinSdk.cdnUrl)
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

            // Update translation files task
            project.tasks.register(
                "downloadTranslationFilesFromWeblate",
                DownloadTranslationFilesTask::class.java
            ) { task ->
                task.cdnUrl.set(extension.translationFiles.cdnUrl)
                task.outputDir.set(extension.translationFiles.outputDir)
            }
        }
    }
}
