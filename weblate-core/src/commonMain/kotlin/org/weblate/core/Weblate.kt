/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import org.weblate.core.annotation.ExperimentalWeblateApi
import org.weblate.core.configuration.Instance
import org.weblate.core.path.DefaultPlatformPath
import org.weblate.core.path.PathUtils
import org.weblate.core.path.PlatformPath

/**
 * Primary way to interact and configure the Weblate SDK
 * @property instance Weblate instance's configuration
 * @property platformPath Path for cache and user directory to store data
 */
@ExperimentalWeblateApi
public class Weblate(
    private val instance: Instance,
    private val platformPath: PlatformPath = DefaultPlatformPath
) {

    private val pathUtils = PathUtils(platformPath)
    private val authClient = HttpClient {
        defaultRequest { url(instance.baseUrl) }
        install(ContentNegotiation) {
            json(json)
            xml()
        }
        install(HttpCache)
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(accessToken = instance.authToken, refreshToken = null)
                }
            }
        }
    }

    /**
     * Saves translations to the user's data directory for the given language
     * @param languageCode Language code for weblate for a particular locale
     * @return [Path] for the downloaded file
     */
    public suspend fun saveTranslation(languageCode: String): Path = authClient
        .prepareGet("translations/${instance.project}/${instance.component}/$languageCode/file/")
        .execute { response ->
            val filePath = Path(pathUtils.getTranslationsDir(languageCode), "strings.xml")
            SystemFileSystem.sink(filePath).buffered().use { sink ->
                response.bodyAsChannel().copyAndClose(sink.asByteWriteChannel())
            }
            return@execute filePath
        }

    public companion object {
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = true
        }
    }
}
