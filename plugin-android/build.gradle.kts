/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

import java.util.Properties
import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.gradle.plugin.publish)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradle.plugin.api)
    implementation(libs.json)
}

// Custom logic to add AGP dependency to class path for testing
val agpTestRuntime = configurations.resolvable("agpTestRuntime") {
    extendsFrom(configurations.compileOnly)
}

val localProperties = Properties().apply {
    val file = layout.projectDirectory.file("../local.properties").asFile
    if (file.exists()) {
        load(file.inputStream())
    }
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(agpTestRuntime)
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter()

            targets.all {
                testTask.configure {
                    environment(
                        "ANDROID_HOME",
                        System.getenv("ANDROID_HOME") ?: localProperties.getProperty("sdk.dir")
                    )
                }
            }
        }
    }
}

gradlePlugin {
    group = "org.weblate"
    version = libs.versions.weblate.get()
    website = "https://weblate.org/"
    vcsUrl = "https://github.com/WeblateOrg/kotlin-sdk"

    plugins {
        register("android") {
            id = "org.weblate.android"
            displayName = "Weblate Gradle Plugin - Android"
            description = "Gradle plugin providing helpful tasks to localize apps with Weblate"
            implementationClass = "org.weblate.plugin.android.WeblateAndroidPlugin"
            tags = listOf("localization", "translations", "android")
            compatibility {
                features {
                    isolatedProjects = true
                    configurationCache = true
                }
            }
        }
    }
}

publishing {
    publications {
        publications.withType<MavenPublication> {
            pom {
                name = "Weblate Gradle Plugin - Android"
                description = "Gradle plugin providing helpful tasks to localize apps with Weblate"
                url = "https://weblate.org/"

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
