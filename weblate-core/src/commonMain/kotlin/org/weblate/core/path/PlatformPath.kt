/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.path

import kotlinx.io.files.Path

/**
 * Path to store files
 */
internal expect object PlatformPath {
    val cacheDir: Path
    val userDir: Path
}
