/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.weblate.plugin.android.Constants.FILE_METADATA

/**
 * Task to upload metadata file to Weblate server
 * @see GenerateMetadataTask
 */
internal abstract class UploadMetadataTask : DefaultTask() {

    @get:Input
    abstract val authToken: Property<String>

    @get:Input
    abstract val apiUrl: Property<String>

    @get:Input
    abstract val versionCodes: ListProperty<Int>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val metadataDir: DirectoryProperty

    @TaskAction
    fun upload() {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build()

        val token = authToken.get()
        val url = apiUrl.get()
        val rootDir = metadataDir.get().asFile
        val versions = versionCodes.get()

        if (!rootDir.exists() || !rootDir.isDirectory) {
            logger.warn("Invalid metadata directory: ${rootDir.absolutePath}")
            return
        }

        val versionDirectories = rootDir.listFiles()?.filter { file ->
            file.isDirectory && file.name.all { it.isDigit() } && file.name.toInt() in versions
        }.orEmpty()

        if (versionDirectories.isEmpty()) {
            logger.warn("No version code subdirectories found in ${rootDir.absolutePath}")
            return
        }

        versionDirectories.forEach { directory ->
            val payload = File(directory, FILE_METADATA).readText()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build()

            logger.warn("Uploading metadata to Weblate")
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            when (response.statusCode()) {
                200, 202 -> logger.warn("Successfully uploaded metadata to Weblate")
                409 -> logger.error("Metadata already exists! Did you forget to increase version code?")
                else -> logger.error("Got an unexpected response: ${response.body()}")
            }
        }
    }

    companion object {
        const val TASK_DESCRIPTION = "Uploads JSON metadata to Weblate"
    }
}
