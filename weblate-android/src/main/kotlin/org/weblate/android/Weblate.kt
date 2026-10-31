/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.android

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.ServiceLoader
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.weblate.android.service.WeblateJobService

/**
 * Primary way to interact with the Weblate library.
 * @param context Context from Android, preferably ApplicationContext
 * @param weblateDir Directory where the configuration is stored, defaults to app's internal storage
 */
@RequiresApi(Build.VERSION_CODES.R)
public class Weblate(
    private val context: Context,
    private val weblateDir: File = File(context.filesDir, DIR_WEBLATE)
) {
    private val TAG = Weblate::class.java.simpleName

    // TODO: Generate it in WeblateConfig
    private val packageName = context.packageName
    private val versionCode = context.packageManager
        .getPackageInfo(packageName, 0)
        .longVersionCode

    private val resourcesLoader = ResourcesLoader()
    private val jobScheduler =
        context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

    private val resourcesDir: File
        get() = File(weblateDir, DIR_RESOURCES)

    private val resourceFile: File
        get() = File(resourcesDir, "$versionCode/$FILE_RESOURCES")

    private var resourceProvider: ResourcesProvider? = null

    init {
        resourcesDir.mkdirs()
        deleteOldResources()
        context.resources.addLoaders(resourcesLoader)
        loadResourcesIfExists()
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
        if (isJobAlreadyScheduled(JOB_ID_WEBLATE_PERIODIC)) {
            Log.i(TAG, "Periodic localization update already enqueued. Skipping...")
            return
        }

        val componentName = ComponentName(context, WeblateJobService::class.java)
        val jobBuilder = JobInfo.Builder(JOB_ID_WEBLATE_PERIODIC, componentName)
            .setPeriodic(TimeUnit.DAYS.toMillis(1), TimeUnit.HOURS.toMillis(1))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            .setBackoffCriteria(TimeUnit.HOURS.toMillis(4), JobInfo.BACKOFF_POLICY_LINEAR)

        Log.i(TAG, "Scheduling periodic localization updates!")
        jobScheduler.schedule(jobBuilder.build())
    }

    /**
     * Cancels previously scheduled daily localization update
     */
    public fun cancelDailyLocalizationUpdate() {
        jobScheduler.cancel(JOB_ID_WEBLATE_PERIODIC)
    }

    /**
     * Triggers an immediate one-time localization update for current locale
     */
    public fun triggerLocalizationUpdate() {
        if (isJobAlreadyScheduled(JOB_ID_WEBLATE_ONESHOT)) {
            Log.i(TAG, "One-time localization update already enqueued. Skipping...")
            return
        }

        val componentName = ComponentName(context, WeblateJobService::class.java)
        val jobBuilder = JobInfo.Builder(JOB_ID_WEBLATE_ONESHOT, componentName).apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> setExpedited(true)
                else -> setMinimumLatency(0)
            }
        }

        jobScheduler.schedule(jobBuilder.build())
    }

    /**
     * Cancels the ongoing one-time localization update
     */
    public fun cancelLocalizationUpdate() {
        jobScheduler.cancel(JOB_ID_WEBLATE_ONESHOT)
    }

    /**
     * Downloads public manifest of resources pointing to localization updates from CDN server
     */
    private suspend fun downloadManifest(): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("${configProvider.cdnUrl}/${packageName}/$versionCode/$FILE_MANIFEST")
                with((url.openConnection() as HttpsURLConnection)) {
                    connectTimeout = 20 * 1000
                    readTimeout = 20 * 1000

                    JSONObject(getInputStream().bufferedReader().readText())
                }
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
    private fun loadResourcesIfExists() {
        if (resourceFile.exists()) {
            resourceProvider = ResourcesProvider.loadFromTable(
                ParcelFileDescriptor.open(resourceFile, ParcelFileDescriptor.MODE_READ_ONLY),
                null
            ).also { provider -> resourcesLoader.addProvider(provider) }
        }
    }

    private fun deleteOldResources() {
        Log.i(TAG, "Deleting old weblate resources")
        resourcesDir.listFiles()
            ?.filter { file -> file.isDirectory && file.name != versionCode.toString() }
            ?.forEach { file -> file.deleteRecursively() }
    }

    /**
     * Downloads the given resources artifact
     */
    private suspend fun downloadArtifact(fileName: String) {
        val tmpFile = File(resourcesDir, "$FILE_RESOURCES.tmp")
        val messageDigest = MessageDigest.getInstance("SHA-256")

        withContext(Dispatchers.IO) {
            try {
                val url = URL("${configProvider.cdnUrl}/artifacts/${fileName}.arsc")
                with((url.openConnection() as HttpsURLConnection)) {
                    connectTimeout = 20 * 1000
                    readTimeout = 20 * 1000

                    DigestInputStream(getInputStream(), messageDigest).use { inputStream ->
                        tmpFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                // Validate hash to ensure file isn't corrupted
                require(messageDigest.digest().toHexString() == fileName)

                if (resourceFile.exists()) {
                    resourceProvider?.let { provider -> resourcesLoader.removeProvider(provider) }
                    resourceFile.delete()
                }

                tmpFile.renameTo(resourceFile)
                loadResourcesIfExists()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to download resources artifact", exception)
            } finally {
                if (tmpFile.exists()) tmpFile.delete()
            }
        }
    }

    /**
     * Whether given job is already scheduled
     */
    private fun isJobAlreadyScheduled(id: Int): Boolean {
        return jobScheduler.allPendingJobs.any { jobInfo -> jobInfo.id == id }
    }

    public companion object {
        private const val JOB_ID_OFFSET = 50_000

        /**
         * Job ID of periodic jobs for checking localization updates
         */
        public const val JOB_ID_WEBLATE_PERIODIC: Int = JOB_ID_OFFSET + 1

        /**
         * Job ID of one-shot jobs for checking localization updates
         */
        public const val JOB_ID_WEBLATE_ONESHOT: Int = JOB_ID_OFFSET + 2

        private const val DIR_WEBLATE = "weblate"
        private const val DIR_RESOURCES = "resources"

        private const val FILE_MANIFEST = "manifest.json"
        private const val FILE_RESOURCES = "resources.arsc"

        private val configProvider: ConfigProvider by lazy {
            ServiceLoader.load(ConfigProvider::class.java, ConfigProvider::class.java.classLoader)
                .first()
        }
    }
}
