/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.weblate.core.utils.AppContextWrapper

internal actual object DriverFactory {

    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        WeblateDB.Schema,
        AppContextWrapper.appContext!!,
        DatabaseHelper.FILE_NAME
    )
}
