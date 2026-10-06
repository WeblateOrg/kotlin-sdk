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
import org.json.JSONObject
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
            throw Exception("Invalid metadata directory: ${rootDir.absolutePath}")
        }

        val versionDirectories = rootDir.listFiles()?.filter { file ->
            file.isDirectory && file.name.all { it.isDigit() } && file.name.toInt() in versions
        }.orEmpty()

        val versionMetadataFiles = versionDirectories.mapNotNull { dir ->
            val file = File(dir, FILE_METADATA)
            if (file.exists()) file else null
        }

        if (versionMetadataFiles.isEmpty()) {
            throw Exception("No metadata file found to publish in ${rootDir.absolutePath}")
        }

        versionDirectories.forEach { directory ->
            val version = directory.name
            val payload = File(directory, FILE_METADATA).readText()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build()

            logger.warn("[WARNING]: Uploading metadata for version:$version")
            val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .join()

            when (response.statusCode()) {
                200 -> logger.warn("[WARNING]: Found existing identical metadata for version:$version")
                202 -> logger.warn("[WARNING]: Successfully uploaded metadata for version:$version")
                409 -> throw Exception("[ERROR]: Found conflicting metadata for version:${directory.name}!")
                else -> throw Exception(JSONObject(response.body()).toString(2))
            }
        }
    }

    companion object {
        const val TASK_DESCRIPTION = "Uploads JSON metadata to Weblate"
    }
}
