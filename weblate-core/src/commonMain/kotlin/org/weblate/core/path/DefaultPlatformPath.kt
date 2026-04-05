/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.path

import kotlinx.io.files.Path

/**
 * Default path to store files
 */
internal expect object DefaultPlatformPath : PlatformPath {
    override val cacheDir: Path
    override val userDir: Path
}
