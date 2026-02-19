/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Primary way to interact and configure the Weblate SDK
 * @property project Project URL slug
 * @property component Component URL slug
 * @property authToken Authentication token to interact with the API
 */
public class Weblate(
    public val project: String,
    public val component: String,
    private val authToken: String
) {

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

    /**
     * Downloads the translation file for given language
     * @param code Language code for weblate for a particular locale
     * @return [ByteArray] for further parsing based on platform-specific parser as needed
     */
    public suspend fun getTranslationForLanguage(code: String): ByteArray = authClient
        .get(urlString = "$$BASE_URL/translations/$project/$component/$code/file/")
        .body()

    public companion object {
        private const val BASE_URL = "https://hosted.weblate.org/api"

        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = true
        }
    }
}
