/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.sdk

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Primary way to interact and configure the Weblate SDK
 * @property project Project URL slug
 * @property authToken Authentication token to interact with the API
 */
public class Weblate(
    public val project: String,
    private val authToken: String
) {

    init {
        require(authToken.startsWith("wlp_")) {
            "Using non-project scoped authentication token!"
        }
    }

    private val authClient = HttpClient {
        defaultRequest { url(BASE_URL) }
        install(ContentNegotiation) { json(json) }
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(accessToken = authToken, refreshToken = null)
                }
            }
        }
    }

    public companion object {
        private const val BASE_URL = "https://weblate.org/api"

        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = true
        }
    }
}
