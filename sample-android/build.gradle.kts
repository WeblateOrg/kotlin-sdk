/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.compose)
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
    implementation(projects.weblateAndroid)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.compose.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.tooling)
    debugImplementation(libs.androidx.compose.test.manifest)
}
