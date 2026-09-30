/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.sample

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WeblateTheme {
                PrimaryScreen()
            }
        }
    }
}

@Composable
fun PrimaryScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    ScreenContent(
        onUpdateResources = {
            viewModel.updateResources()
        },
        onSwitchActivity = {
            Intent(context, ChildActivity::class.java).also { intent ->
                context.startActivity(intent)
            }
        }
    )
}

@Composable
fun ScreenContent(onUpdateResources: () -> Unit = {}, onSwitchActivity: () -> Unit = {}) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues.calculateTopPadding())
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.sdk_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.sdk_summary),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.weblate_description),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.sdk_description),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = pluralStringResource(R.plurals.sdk_advertisement, 10),
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = onUpdateResources,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            ) {
                Text(text = stringResource(R.string.update))
            }

            Button(onClick = onSwitchActivity) {
                Text(text = "Switch to Java")
            }
        }
    }
}

@Composable
fun WeblateTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Preview
@Composable
private fun PrimaryScreenPreview() {
    ScreenContent()
}
