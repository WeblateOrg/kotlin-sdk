/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.plugin.android

import kotlinx.serialization.json.Json

internal object Constants {

    /**
     * Reusable configuration for JSON serialization
     */
    val Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }
}
