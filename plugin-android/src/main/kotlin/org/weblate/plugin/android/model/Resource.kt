/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.plugin.android.model

/**
 * Types of resources to handle when parsing resources
 */
internal enum class Resource(val id: String) {
    STRING(id = "string"),
    PLURAL(id = "plurals")
}
