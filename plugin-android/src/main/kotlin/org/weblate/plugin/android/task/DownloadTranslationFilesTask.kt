/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android.task

import java.io.File
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.json.JSONObject
import org.weblate.plugin.android.Constants
import org.weblate.plugin.android.Constants.httpClient

/**
 * Task to download translation files published by CDN Add-on on the Weblate server
 */
internal abstract class DownloadTranslationFilesTask : DefaultTask() {

    @get:Input
    abstract val excludedLocales: ListProperty<String>

    @get:Input
    abstract val cdnUrl: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        group = Constants.WEBLATE_TASK_GROUP
        description = TASK_DESCRIPTION
    }

    @TaskAction
    fun updateTranslationFiles() {
        val baseUrl = cdnUrl.get().removeSuffix("/")
        val resDir = outputDir.get().asFile

        val manifestRequest = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/$FILE_MANIFEST"))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .GET()
            .build()

        logger.warn("[WARNING]: Requesting manifest for translation files from CDN server")
        val response = httpClient()
            .sendAsync(manifestRequest, HttpResponse.BodyHandlers.ofString())
            .join()

        val jsonResponse = JSONObject(response.body())
        when (response.statusCode()) {
            200 -> downloadTranslationFiles(jsonResponse, baseUrl, resDir)
            else -> error(jsonResponse.toString(2))
        }
    }

    private fun downloadTranslationFiles(manifest: JSONObject, url: String, resDir: File) {
        val locales = manifest.getJSONObject("locales")
        val excludedLocales = excludedLocales.get()

        if (excludedLocales.isNotEmpty()) {
            logger.warn("[WARNING]: Skipping following locales from download: $excludedLocales")
        }

        val filteredLocales = locales.keySet().filter { locale -> locale !in excludedLocales }
        if (filteredLocales.isEmpty()) {
            logger.warn("[WARNING]: No translation files available to download")
            return
        }

        filteredLocales.forEach { locale ->
            val files = locales.getJSONArray(locale)

            // Can there be multiple translation files for one locale?
            assert(files.length() == 1) { "Found more than one file for $locale" }

            val jsonFile = files.getJSONObject(0)
            val localeResDir = resDir.resolve("values-$locale").apply { mkdirs() }
            val localeResFile = localeResDir.resolve("strings.xml")

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$url/${jsonFile.getString("url")}"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/xml")
                .GET()
                .build()

            logger.warn("[WARNING]: Downloading translation file for $locale")
            val response = httpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .join()

            when (response.statusCode()) {
                200 -> localeResFile.writeText(response.body())
                else -> error(JSONObject(response.body()).toString(2))
            }
        }
    }

    companion object {
        private const val FILE_MANIFEST = "manifest.json"
        private const val TASK_DESCRIPTION = "Downloads translation files from Weblate"
    }
}
