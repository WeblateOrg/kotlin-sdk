/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.sample

import android.app.Application
import org.weblate.android.Weblate

class WeblateApp : Application() {

    lateinit var weblate: Weblate

    override fun onCreate() {
        super.onCreate()
        weblate = Weblate(this)
    }
}
