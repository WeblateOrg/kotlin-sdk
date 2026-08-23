/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.weblate.core.app.AppConfiguration
import org.weblate.core.database.translation.Translation
import org.weblate.core.database.translation.TranslationDao

internal const val FILE_DATABASE = "weblate.db"

@Database(entities = [Translation::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
internal abstract class WeblateDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
internal expect object AppDatabaseConstructor : RoomDatabaseConstructor<WeblateDatabase> {
    override fun initialize(): WeblateDatabase
}

/**
 * Database builder for room, see actual implementation for platform-specific details
 */
internal expect fun databaseBuilder(
    appConfiguration: AppConfiguration
): RoomDatabase.Builder<WeblateDatabase>

/**
 * Returns an instance of the Weblate database
 */
internal fun database(builder: RoomDatabase.Builder<WeblateDatabase>): WeblateDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
