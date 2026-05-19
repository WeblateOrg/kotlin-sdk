/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.app

import kotlinx.io.files.Path

/**
 * Configuration related to app required by the library to store files
 */
public interface AppConfiguration {
    public val appName: String
    public val cacheDir: Path
    public val userDir: Path
}
