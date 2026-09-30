/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android.model

/**
 * Types of resources to handle when parsing resources
 */
internal enum class Resource(val id: String) {
    STRING(id = "string"),
    PLURAL(id = "plurals")
}
