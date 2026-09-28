/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.android

import android.content.Context
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.weblate.android.model.Artifact
import org.weblate.android.model.Manifest
import org.weblate.android.work.WeblateWorker
import org.weblate.android.work.WeblateWorker.Companion.ONE_TIME_WEBLATE_WORKER
import org.weblate.android.work.WeblateWorker.Companion.PERIODIC_WEBLATE_WORKER

/**
 * Primary way to interact with the Weblate library.
 */
@OptIn(ExperimentalSerializationApi::class)
public class Weblate(private val context: Context) {

    private val TAG = Weblate::class.java.simpleName

    private val weblateDir: File
        get() = File(context.filesDir, DIR_WEBLATE)

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
     * Downloads localization update for the given locale, if available.
     */
    public suspend fun download(locale: Locale) {
        downloadManifest()?.locales?.get(locale.language)?.let { artifact ->
            Log.i(TAG, "Downloading localization updates for ${locale.displayLanguage}")
            download(artifact)
        }
    }

    /**
     * Schedules daily localization update for current locale
     */
    public fun scheduleLocalizationUpdate(): Operation {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<WeblateWorker>(
            repeatInterval = 1.days.toJavaDuration(),
            flexTimeInterval = 1.hours.toJavaDuration()
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        periodicWorkRequest
            .setBackoffCriteria(BackoffPolicy.LINEAR, 4.hours.toJavaDuration())
            .setConstraints(constraints)

        Log.i(TAG, "Scheduling periodic localization updates!")
        return WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_WEBLATE_WORKER,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest.build()
            )
    }

    /**
     * Triggers an immediate localization update for current locale
     */
    public fun triggerLocalizationUpdate(): Operation {
        val workRequest = OneTimeWorkRequestBuilder<WeblateWorker>()
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .build()

        return WorkManager.getInstance(context)
            .enqueueUniqueWork(ONE_TIME_WEBLATE_WORKER, ExistingWorkPolicy.KEEP, workRequest)
    }

    /**
     * Downloads public manifest of resources pointing to localization updates from CDN server
     */
    private suspend fun downloadManifest(): Manifest? {
        val packageName = context.packageName
        val versionCode = context.packageManager
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
    private fun loadResources() {
        ResourcesLoader().also { resourcesLoader ->
            resourcesLoader.addProvider(
                ResourcesProvider.loadFromDirectory(resourcesDir.path, null)
            )
            context.resources.addLoaders(resourcesLoader)
        }
    }

    /**
     * Downloads the given resources artifact
     */
    private suspend fun download(artifact: Artifact) {
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
