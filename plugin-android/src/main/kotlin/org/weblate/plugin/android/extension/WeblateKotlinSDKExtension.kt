/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android.extension

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Extension to configure values for Kotlin SDK (Add-on)
 */
public interface WeblateKotlinSDKExtension {

    /**
     * Path to output/input metadata file for Weblate, `defaults to build/output/weblate/metadata`
     */
    public val metadataDir: DirectoryProperty

    /**
     * URL of the Kotlin SDK CDN server
     */
    public val cdnUrl: Property<String>
}
