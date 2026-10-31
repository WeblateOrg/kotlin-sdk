/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android.extension

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension to configure values for translation files (Add-on)
 */
public interface WeblateTranslationFilesExtension {

    /**
     * Path to output downloaded translation files from Weblate, defaults to `src/main/res`
     */
    public val outputDir: DirectoryProperty

    /**
     * URL of the translation files CDN server
     */
    public val cdnUrl: Property<String>

    /**
     * Language codes to skip downloading
     */
    public val excludedLocales: ListProperty<String>
}
