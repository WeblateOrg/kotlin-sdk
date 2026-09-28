/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.launch

class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    fun updateResources() {
        viewModelScope.launch {
            val locale = Locale.Builder()
                .setLanguage("hi")
                .setRegion("IN")
                .build()
            app.weblate.updateResources(locale)
        }
    }
}
