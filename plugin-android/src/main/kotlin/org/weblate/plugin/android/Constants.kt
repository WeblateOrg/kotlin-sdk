/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android

import java.net.http.HttpClient
import java.time.Duration

internal object Constants {

    /**
     * Default task group for the plugin
     */
    const val WEBLATE_TASK_GROUP = "weblate"

    /**
     * File name for the JSON metadata
     */
    const val FILE_METADATA = "org.weblate.metadata.json"

    /**
     * HTTP client to make network requests
     */
    internal fun httpClient() = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()
}
