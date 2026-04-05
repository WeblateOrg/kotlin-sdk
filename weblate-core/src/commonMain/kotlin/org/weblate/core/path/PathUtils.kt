/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.path

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

internal object PathUtils {

    private const val DIR_WEBLATE = "weblate"
    private const val DIR_TRANSLATIONS = "translations"

    fun getCacheDir(): Path {
        val path = Path(PlatformPath.cacheDir, DIR_WEBLATE)
        SystemFileSystem.createDirectories(path)
        return path
    }

    fun getUserDir(): Path {
        val path = Path(PlatformPath.userDir, DIR_WEBLATE)
        SystemFileSystem.createDirectories(path)
        return path
    }

    fun getTranslationsDir(languageCode: String): Path {
        val path = Path(getUserDir(), DIR_TRANSLATIONS, languageCode)
        SystemFileSystem.createDirectories(path)
        return path
    }
}
