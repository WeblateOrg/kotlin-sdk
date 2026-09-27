/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.android

import android.app.Application
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.util.Locale
import java.util.ServiceLoader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.weblate.android.model.Artifact
import org.weblate.android.model.Manifest

/**
 * Primary way to interact with the Weblate library
 */
@OptIn(ExperimentalSerializationApi::class)
public class Weblate(private val application: Application) {

    private val TAG = Weblate::class.java.simpleName

    private val weblateDir: File
        get() = File(application.filesDir, DIR_WEBLATE)

    private val configDir: File
        get() = File(weblateDir, DIR_CONFIG)

    private val resourcesDir: File
        get() = File(weblateDir, DIR_RESOURCES)

    private val manifest: File
        get() = File(configDir, FILE_MANIFEST)

    private val resources: File
        get() = File(resourcesDir, FILE_RESOURCES)

    init {
        configDir.mkdirs()
        resourcesDir.mkdirs()

        loadResources()
    }

    /**
     * Downloads localization updates for the given locale, if available.
     */
    public suspend fun updateResources(locale: Locale) {
        downloadManifest()?.locales?.get(locale.language)?.let { artifact ->
            Log.i(TAG, "Downloading localization updates for ${locale.displayLanguage}")
            download(artifact)
        }
    }

    /**
     * Downloads public manifest of resources pointing to localization updates from CDN server
     */
    internal suspend fun downloadManifest(): Manifest? {
        val packageName = application.packageName
        val versionCode = application.packageManager
            .getPackageInfo(packageName, 0)
            .longVersionCode

        return httpClient
            .prepareGet("${configProvider.cdnUrl}/${packageName}/$versionCode/$FILE_MANIFEST")
            .execute { response ->
                return@execute when (response.status) {
                    HttpStatusCode.OK -> {
                        manifest.writeBytes(response.bodyAsBytes())
                        json.decodeFromStream(manifest.inputStream())
                    }

                    else -> {
                        Log.e(TAG, "Failed to download manifest: ${response.status.description}")
                        null
                    }
                }
            }
    }

    /**
     * Hooks weblate resources directory to application's resource loader for loading localization
     * updates
     */
    internal fun loadResources() {
        ResourcesLoader().also { resourcesLoader ->
            resourcesLoader.addProvider(
                ResourcesProvider.loadFromDirectory(resourcesDir.path, null)
            )
            application.resources.addLoaders(resourcesLoader)
        }
    }

    /**
     * Downloads the given resources artifact
     */
    internal suspend fun download(artifact: Artifact) {
        httpClient.prepareGet("${configProvider.cdnUrl}/artifacts/${artifact.sha256}.arsc")
            .execute { response ->
                when (response.status) {
                    HttpStatusCode.OK -> resources.writeBytes(response.bodyAsBytes())

                    else -> {
                        Log.e(TAG, "Failed to download resources: ${response.status.description}")
                    }
                }
            }
    }

    internal companion object {
        private const val DIR_WEBLATE = "weblate"
        private const val DIR_CONFIG = "config"
        private const val DIR_RESOURCES = "resources"

        private const val FILE_MANIFEST = "manifest.json"
        private const val FILE_RESOURCES = "resources.arsc"

        private val configProvider: ConfigProvider by lazy {
            ServiceLoader.load(ConfigProvider::class.java, ConfigProvider::class.java.classLoader)
                .first()
        }

        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = true
        }

        private val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpCache)
        }
    }
}
