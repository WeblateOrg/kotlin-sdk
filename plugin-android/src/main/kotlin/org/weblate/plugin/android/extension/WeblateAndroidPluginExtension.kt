/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android.extension

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * Root extension to configure the Weblate Gradle plugin for the module/subproject
 */
public interface WeblateAndroidPluginExtension {

    /**
     * Project URL slug, e.g. aurora-store
     */
    public val project: Property<String>

    /**
     * Component URL slug, e.g. fastlane
     */
    public val component: Property<String>

    /**
     * Authentication token to interact with the API
     */
    public val authToken: Property<String>

    /**
     * URL of the Weblate server
     */
    public val serverUrl: Property<String>

    /**
     * Kotlin-SDK (Add-on) instance
     */
    @get:Nested
    public val kotlinSdk: WeblateKotlinSDKExtension

    /**
     * Configuration block for Kotlin-SDK (Add-on)
     */
    public fun kotlinSdk(action: Action<WeblateKotlinSDKExtension>) {
        action.execute(kotlinSdk)
    }

    /**
     * Translation files (Add-on) instance
     */
    @get:Nested
    public val translationFiles: WeblateTranslationFilesExtension

    /**
     * Configuration block for translation files (Add-on)
     */
    public fun translationFiles(action: Action<WeblateTranslationFilesExtension>) {
        action.execute(translationFiles)
    }
}
