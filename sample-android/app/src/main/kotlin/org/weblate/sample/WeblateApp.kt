/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.sample

import android.app.Application
import android.os.Build
import org.weblate.android.Weblate

class WeblateApp : Application() {

    lateinit var weblate: Weblate
        private set

    override fun onCreate() {
        super.onCreate()

        // Enables daily localization updates
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            weblate = Weblate(this)
            weblate.scheduleDailyLocalizationUpdate()
        }
    }
}
