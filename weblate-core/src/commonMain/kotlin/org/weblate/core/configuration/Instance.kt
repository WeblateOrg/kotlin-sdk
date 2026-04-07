/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.configuration

/**
 * Configuration of the Weblate instance
 * @property project Project URL slug, e.g. aurora-store
 * @property component Component URL slug, e.g. fastlane
 * @property authToken Authentication token to interact with the API
 * @property serverUrl URL of the Weblate server, defaults to `https://hosted.weblate.org`
 */
public class Instance(
    public val project: String,
    public val component: String,
    public val authToken: String,
    public val serverUrl: String = DEFAULT_SERVER_URL
) {
    internal val baseUrl: String = "$serverUrl/api"

    public companion object {
        public const val DEFAULT_SERVER_URL: String = "https://hosted.weblate.org/"
    }
}
