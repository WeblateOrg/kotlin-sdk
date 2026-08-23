/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.io.files.Path
import org.weblate.core.app.AppConfiguration
import org.weblate.core.app.PathUtils

/**
 * Builder for room database on iOS
 */
internal actual fun databaseBuilder(
    appConfiguration: AppConfiguration
): RoomDatabase.Builder<WeblateDatabase> {
    val pathUtils = PathUtils(appConfiguration)
    val databasePath = Path(pathUtils.getDatabaseDir(), FILE_DATABASE)
    return Room.databaseBuilder(
        name = databasePath.toString()
    )
}
