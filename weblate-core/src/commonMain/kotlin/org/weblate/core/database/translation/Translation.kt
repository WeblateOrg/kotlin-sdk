/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.core.database.translation

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue

@Entity
@Serializable
@XmlSerialName("string")
internal data class Translation(
    @PrimaryKey val name: String,
    @XmlValue val value: String,
    val translatable: Boolean = true,
    val type: Type = Type.UNKNOWN
) {
    enum class Type {
        GENERAL,
        UNKNOWN
    }
}
