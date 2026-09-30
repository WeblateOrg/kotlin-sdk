/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android.model

/**
 * Class to hold metadata about strings of an app for Weblate server
 */
internal data class Metadata(
    val packageName: String,
    val versionCode: Int,
    val strings: Map<String, String>,
    val plurals: Map<String, String>
)
