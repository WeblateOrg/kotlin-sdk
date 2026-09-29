/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.android

import android.content.Context
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.net.URL
import java.util.Locale
import java.util.ServiceLoader
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.weblate.android.work.WeblateWorker

/**
 * Primary way to interact with the Weblate library.
 */
@RequiresApi(Build.VERSION_CODES.R)
public class Weblate(private val context: Context) {

    private val TAG = Weblate::class.java.simpleName

    // TODO: Generate it in WeblateConfig
    private val packageName = context.packageName
    private val versionCode = context.packageManager
        .getPackageInfo(packageName, 0)
        .longVersionCode

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
            .resolve(versionCode.toString())

    init {
        configDir.mkdirs()
        resourcesDir.mkdirs()

        loadResources()
    }

    /**
     * Downloads localization update for the given locale, if available.
     */
    public suspend fun download(locale: Locale) {
        downloadManifest()
            ?.getJSONObject("locales")
            ?.getJSONObject(locale.language)
            ?.getString("sha256") // sha256 also serves as the fileName for the artifact
            ?.let { fileName ->
                Log.i(TAG, "Downloading localization updates for ${locale.language}")
                downloadArtifact(fileName)
            }
    }

    /**
     * Schedules daily localization update for current locale
     */
    public fun scheduleDailyLocalizationUpdate() {
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
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_WEBLATE_WORKER,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest.build()
            )
    }

    /**
     * Cancels previously scheduled daily localization update
     */
    public fun cancelDailyLocalizationUpdate() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(PERIODIC_WEBLATE_WORKER)
    }

    /**
     * Triggers an immediate one-time localization update for current locale
     */
    public fun triggerLocalizationUpdate() {
        val workRequest = OneTimeWorkRequestBuilder<WeblateWorker>()
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONE_TIME_WEBLATE_WORKER, ExistingWorkPolicy.KEEP, workRequest)
    }

    /**
     * Cancels the ongoing one-time localization update
     */
    public fun cancelLocalizationUpdate() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(ONE_TIME_WEBLATE_WORKER)
    }

    /**
     * Downloads public manifest of resources pointing to localization updates from CDN server
     */
    private suspend fun downloadManifest(): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("${configProvider.cdnUrl}/${packageName}/$versionCode/$FILE_MANIFEST")
                url.openStream().use { inputStream ->
                    manifest.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                JSONObject(manifest.readText())
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download manifest", exception)
                null
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
    private suspend fun downloadArtifact(fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("${configProvider.cdnUrl}/artifacts/${fileName}.arsc")
                url.openStream().use { inputStream ->
                    resources.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download resources artifact", exception)
            }
        }
    }

    public companion object {
        public const val PERIODIC_WEBLATE_WORKER: String = "PERIODIC_WEBLATE_WORKER"
        public const val ONE_TIME_WEBLATE_WORKER: String = "ONE_TIME_WEBLATE_WORKER"

        private const val DIR_WEBLATE = "weblate"
        private const val DIR_CONFIG = "config"
        private const val DIR_RESOURCES = "resources"

        private const val FILE_MANIFEST = "manifest.json"
        private const val FILE_RESOURCES = "resources.arsc"

        private val configProvider: ConfigProvider by lazy {
            ServiceLoader.load(ConfigProvider::class.java, ConfigProvider::class.java.classLoader)
                .first()
        }
    }
}
