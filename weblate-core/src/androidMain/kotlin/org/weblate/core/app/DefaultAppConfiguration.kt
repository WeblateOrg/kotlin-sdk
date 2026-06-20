/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.app

import kotlinx.io.files.Path
import org.weblate.core.Constants.DIR_WEBLATE
import org.weblate.core.utils.AppContextWrapper

/**
 * Default app's configuration provider for Android
 */
internal object DefaultAppConfiguration : AppConfiguration {

    override val appName: String
        get() {
            val context = AppContextWrapper.appContext!!
            return context.applicationInfo.loadLabel(context.packageManager).toString()
        }

    override val cacheDir: Path
        get() = Path(AppContextWrapper.appContext!!.cacheDir.absolutePath, DIR_WEBLATE)

    override val userDir: Path
        get() = Path(AppContextWrapper.appContext!!.filesDir.absolutePath, DIR_WEBLATE)
}
