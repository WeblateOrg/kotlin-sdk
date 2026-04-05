/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.path

import kotlinx.io.files.Path
import org.weblate.core.Constants.DIR_WEBLATE
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal actual object DefaultPlatformPath : PlatformPath {

    actual override val cacheDir: Path
        get() = Path(
            NSSearchPathForDirectoriesInDomains(
                NSCachesDirectory,
                NSUserDomainMask,
                true
            ).first() as String,
            DIR_WEBLATE
        )

    actual override val userDir: Path
        get() = Path(
            NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            ).first() as String,
            DIR_WEBLATE
        )
}
