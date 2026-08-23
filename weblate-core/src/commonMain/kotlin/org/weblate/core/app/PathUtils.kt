/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.app

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

internal class PathUtils(private val appConfiguration: AppConfiguration) {

    init {
        listOf(appConfiguration.cacheDir, appConfiguration.userDir).forEach { path ->
            SystemFileSystem.createDirectories(path)
        }
    }

    fun getTranslationsDir(languageCode: String): Path {
        return Path(appConfiguration.userDir, DIR_TRANSLATIONS, languageCode).also { path ->
            SystemFileSystem.createDirectories(path)
        }
    }

    fun getDatabaseDir(): Path {
        return Path(appConfiguration.userDir, DIR_DATABASE).also { path ->
            SystemFileSystem.createDirectories(path)
        }
    }

    companion object {
        private const val DIR_TRANSLATIONS = "translations"
        private const val DIR_DATABASE = "database"
    }
}
