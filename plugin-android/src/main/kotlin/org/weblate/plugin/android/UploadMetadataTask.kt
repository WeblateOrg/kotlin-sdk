/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.plugin.android

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Task to upload metadata file to Weblate server
 * @see GenerateMetadataTask
 */
internal abstract class UploadMetadataTask : DefaultTask() {

    @get:Input
    abstract val authToken: Property<String>

    @get:Input
    abstract val apiUrl: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val metadataFile: RegularFileProperty

    @TaskAction
    fun upload() {
        val token = authToken.get()
        val url = apiUrl.get()
        val payload = metadataFile.get().asFile.readText()

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        logger.warn("Uploading metadata to Weblate")
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        when (response.statusCode()) {
            200, 202 -> logger.warn("Successfully uploaded metadata to Weblate")
            409 -> logger.error("Metadata already exists! Did you forget to increase version code?")
            else -> logger.error("Got an unexpected response: ${response.body()}")
        }
    }

    companion object {
        const val TASK_DESCRIPTION = "Uploads JSON metadata to Weblate"
    }
}
