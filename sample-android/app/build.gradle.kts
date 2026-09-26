/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

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
    implementation(libs.androidx.core)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.compose.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.tooling)
    debugImplementation(libs.androidx.compose.test.manifest)
}

weblate {
    serverUrl = "https://hosted.weblate.org"
    cdnUrl = "https://weblate-cdn.com/c6e2de08693e4fb8bba1ecfae9a8cfd9"
    authToken = "INSERT_TOKEN_HERE"
    project = "sandbox"
    component = "kotlin-sdk"
}
