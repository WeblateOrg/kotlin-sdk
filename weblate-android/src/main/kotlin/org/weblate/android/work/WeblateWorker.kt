/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.android.work

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Locale
import org.weblate.android.Weblate

/**
 * Worker to download localization updates from Weblate server
 */
internal class WeblateWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    private val TAG = WeblateWorker::class.java.simpleName
    private val weblate = Weblate(context.applicationContext as Application)

    override suspend fun doWork(): Result {
        Log.i(TAG, "Checking localization updates for current locale")

        try {
            weblate.download(Locale.getDefault())
            return Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to check localization updates", exception)
            return Result.failure()
        }
    }
}
