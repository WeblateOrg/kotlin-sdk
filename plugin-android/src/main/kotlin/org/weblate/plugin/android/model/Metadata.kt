/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.plugin.android.model

import kotlinx.serialization.Serializable

/**
 * Class to hold metadata about strings of an app for Weblate server
 */
@Serializable
internal data class Metadata(
    val packageName: String,
    val versionCode: Long,
    val strings: Map<String, String>,
    val plurals: Map<String, String>
)
