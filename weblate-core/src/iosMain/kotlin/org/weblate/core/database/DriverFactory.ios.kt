/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

internal actual object DriverFactory {

    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        WeblateDB.Schema,
        DatabaseHelper.FILE_NAME
    )
}
