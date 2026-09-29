/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.android.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.weblate.android.Weblate

/**
 * Job to download localization updates from Weblate server
 */
internal class WeblateJobService: JobService() {

    private val TAG = WeblateJobService::class.java.simpleName
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartJob(params: JobParameters?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.e(TAG, "Job was scheduled on unsupported android versions, exiting!")
            return true
        }

        Log.i(TAG, "Checking localization updates for current locale")
        serviceScope.launch {
            var needsReschedule = false
            try {
                val weblate = Weblate(applicationContext)
                weblate.download(Locale.getDefault())
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to check localization updates", exception)
                needsReschedule = true
            } finally {
                jobFinished(params, needsReschedule)
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceScope.cancel("Job interrupted by system")
        return false
    }
}
