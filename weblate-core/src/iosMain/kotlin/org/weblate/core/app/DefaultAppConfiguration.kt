/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.app

import kotlinx.io.files.Path
import org.weblate.core.Constants.DIR_WEBLATE
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Default app's configuration provider for iOS
 */
public object DefaultAppConfiguration : AppConfiguration {

    override val appName: String
        get() {
            val bundle = NSBundle.mainBundle
            val displayName = bundle.objectForInfoDictionaryKey("CFBundleDisplayName") as? String
            val bundleName = bundle.objectForInfoDictionaryKey("CFBundleName") as? String
            return when {
                !displayName.isNullOrBlank() -> displayName
                !bundleName.isNullOrBlank() -> bundleName
                else -> "Unknown"
            }
        }

    override val cacheDir: Path
        get() = Path(
            NSSearchPathForDirectoriesInDomains(
                NSCachesDirectory,
                NSUserDomainMask,
                true
            ).first() as String,
            DIR_WEBLATE
        )

    override val userDir: Path
        get() = Path(
            NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            ).first() as String,
            DIR_WEBLATE
        )
}
