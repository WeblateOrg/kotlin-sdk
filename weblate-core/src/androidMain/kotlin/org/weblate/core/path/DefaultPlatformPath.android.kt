/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.path

import kotlinx.io.files.Path
import org.weblate.core.Constants.DIR_WEBLATE
import org.weblate.core.utils.AppContextWrapper

internal actual object DefaultPlatformPath : PlatformPath {

    actual override val cacheDir: Path
        get() = Path(AppContextWrapper.appContext!!.cacheDir.absolutePath, DIR_WEBLATE)

    actual override val userDir: Path
        get() = Path(AppContextWrapper.appContext!!.filesDir.absolutePath, DIR_WEBLATE)
}
