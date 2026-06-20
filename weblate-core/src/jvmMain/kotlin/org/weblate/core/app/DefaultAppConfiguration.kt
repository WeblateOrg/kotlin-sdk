/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.app

import kotlinx.io.files.Path
import org.weblate.core.Constants.DIR_WEBLATE

/**
 * Default app's configuration provider for JVM
 */
public class DefaultAppConfiguration(override val appName: String) : AppConfiguration {

    private val os = System.getProperty("os.name").lowercase()
    private val userHome = System.getProperty("user.home")

    override val cacheDir: Path
        get() {
            val path = when {
                os.contains("win") -> System.getenv("LOCALAPPDATA") ?: "$userHome/AppData/Local"
                os.contains("mac") -> "$userHome/Library/Caches"
                else -> System.getenv("XDG_CACHE_HOME") ?: "$userHome/.cache"
            }
            return Path(path, appName, DIR_WEBLATE)
        }

    override val userDir: Path
        get() {
            val path = when {
                os.contains("win") -> System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
                os.contains("mac") -> "$userHome/Library/Application Support"
                else -> System.getenv("XDG_DATA_HOME") ?: "$userHome/.local/share"
            }
            return Path(path, appName, DIR_WEBLATE)
        }
}
