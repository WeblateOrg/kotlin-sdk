/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.path

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

internal class PathUtils(private val platformPath: PlatformPath) {

    init {
        listOf(platformPath.cacheDir, platformPath.userDir).forEach { path ->
            SystemFileSystem.createDirectories(path)
        }
    }

    fun getTranslationsDir(languageCode: String): Path {
        val path = Path(platformPath.userDir, DIR_TRANSLATIONS, languageCode)
        SystemFileSystem.createDirectories(path)
        return path
    }

    companion object {
        private const val DIR_TRANSLATIONS = "translations"
    }
}
