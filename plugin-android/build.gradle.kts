/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    `maven-publish`
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradle.plugin.api)
    implementation(libs.jetbrains.kotlin.serialization)
}

gradlePlugin {
    group = "org.weblate"
    version = "1.0.0"
    website = "https://weblate.org/"
    vcsUrl = "https://github.com/WeblateOrg/kotlin-sdk"

    plugins {
        register("android") {
            id = "org.weblate.plugin.android"
            implementationClass = "org.weblate.plugin.android.WeblateAndroidPlugin"
        }
    }
}

publishing {
    publications {
        publications.withType<MavenPublication> {
            pom {
                name = "Weblate Gradle Plugin - Android"
                description = "Gradle plugin providing helpful tasks to localize apps with Weblate"
                url = "https://github.com/WeblateOrg/kotlin-sdk"

                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                scm {
                    url = "https://github.com/WeblateOrg/kotlin-sdk"
                    connection = "scm:git:git@github.com:WeblateOrg/kotlin-sdk.git"
                    developerConnection = "scm:git:git@github.com:WeblateOrg/kotlin-sdk.git"
                }

                developers {
                    developer {
                        id = "weblate"
                        name = "Weblate"
                        email = "info@weblate.org"
                    }
                }
            }
        }
    }
}
