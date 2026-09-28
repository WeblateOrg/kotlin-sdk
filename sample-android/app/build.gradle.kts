/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import com.android.build.api.variant.FilterConfiguration.FilterType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.weblate.android)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "org.weblate.sample"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "org.weblate.sample"
        minSdk {
            version = release(30)
        }
        targetSdk {
            version = release(37)
        }
        versionCode = 1
        versionName = "1.0.0"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(libs.weblate.android)

    implementation(libs.androidx.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.compose.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel)

    debugImplementation(libs.androidx.compose.tooling)
    debugImplementation(libs.androidx.compose.test.manifest)
}

// https://developer.android.com/build/configure-apk-splits?#configure-APK-versions
androidComponents {
    onVariants { variant ->
        val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType == FilterType.ABI }?.identifier
            val baseAbiCode = abiCodes[abi] ?: 0
            output.versionCode = ((android.defaultConfig.versionCode ?: 0) * 10) + baseAbiCode
        }
    }
}


weblate {
    serverUrl = "https://hosted.weblate.org"
    cdnUrl = "https://weblate-cdn.com/c6e2de08693e4fb8bba1ecfae9a8cfd9"
    authToken = "INSERT_TOKEN_HERE"
    project = "sandbox"
    component = "kotlin-sdk"
}
