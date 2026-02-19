/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.path

import kotlinx.io.files.Path
import org.weblate.core.utils.AppContextWrapper

internal actual object PlatformPath {

    actual val cacheDir: Path
        get() = Path(AppContextWrapper.appContext!!.cacheDir.absolutePath)

    actual val userDir: Path
        get() = Path(AppContextWrapper.appContext!!.filesDir.absolutePath)
}
