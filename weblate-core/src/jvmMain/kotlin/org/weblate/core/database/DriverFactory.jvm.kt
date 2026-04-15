/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

internal actual object DriverFactory {

    actual fun createDriver(): SqlDriver = JdbcSqliteDriver(
        DatabaseHelper.FILE_NAME,
        Properties(),
        WeblateDB.Schema
    )
}
