/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.plugin.android

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Extension to configure the Weblate Gradle plugin
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
     * Path to output/input metadata file for Weblate, `defaults to build/output/weblate`
     */
    public val metadataFile: RegularFileProperty
}
