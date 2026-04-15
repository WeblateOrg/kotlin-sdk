/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Driver to access sqldelight database
 */
internal expect object DriverFactory {
    fun createDriver(): SqlDriver
}
