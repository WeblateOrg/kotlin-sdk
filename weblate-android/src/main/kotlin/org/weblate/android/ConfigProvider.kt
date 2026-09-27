/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.android

/**
 * Contract for providing required configuration for library. See Gradle plugin for generated
 * implementation.
 */
public interface ConfigProvider {
    public val cdnUrl: String
}
