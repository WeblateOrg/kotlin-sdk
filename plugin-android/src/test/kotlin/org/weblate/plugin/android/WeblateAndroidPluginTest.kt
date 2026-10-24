/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WeblateAndroidPluginTest {

    @field:TempDir
    lateinit var projectDir: File

    lateinit var gradleRunner: GradleRunner

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }

    @BeforeEach
    fun setup() {
        gradleRunner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
    }

    @Test
    fun `plugin fails to register tasks without AGP`() {
        // AGP is present in plugin classpath as we have configured it in build.gradle.kts file.
        // The best way to check is whether tasks are getting registered.
        buildFile.writeText(CONTENT_FILE_BUILD_NO_AGP)
        val result = gradleRunner
            .withArguments("-m", "generateMetadataForWeblateDebug")
            .buildAndFail()

        assertTrue(result.output.contains("BUILD FAILED"))
    }

    @Test
    fun `plugin registers tasks successfully with AGP`() {
        buildFile.writeText(CONTENT_FILE_BUILD_NO_CONFIG)
        val result = gradleRunner
            .withArguments("tasks", "--group", "weblate")
            .build()

        assertTrue(result.output.contains("Weblate tasks"))
    }

    @Test
    fun `config task is not registered without cdn URL in extension`() {
        buildFile.writeText(CONTENT_FILE_BUILD_NO_CONFIG)
        val result = gradleRunner
            .withArguments("tasks", "--group", "weblate")
            .build()

        assertTrue(!result.output.contains("generateConfigForWeblate"))
    }

    @Test
    fun `config task is registered with cdn URL in extension`() {
        buildFile.writeText(CONTENT_FILE_BUILD_WITH_CONFIG)
        val result = gradleRunner
            .withArguments("tasks", "--group", "weblate")
            .build()

        assertTrue(result.output.contains("generateConfigForWeblate"))
    }

    @Test
    fun `upload metadata task triggers generate metadata task`() {
        buildFile.writeText(CONTENT_FILE_BUILD_NO_CONFIG)
        val result = gradleRunner
            .withArguments("-m", "uploadMetadataForWeblateDebug")
            .build()

        assertTrue(result.output.contains("generateMetadataForWeblateDebug"))
    }

    @Test
    fun `metadata generation task is triggered automatically on building app`() {
        buildFile.writeText(CONTENT_FILE_BUILD_NO_CONFIG)
        val result = gradleRunner
            .withArguments("-m", "assembleDebug")
            .build()

        assertTrue(result.output.contains("generateMetadataForWeblateDebug"))
    }

    companion object {
        private val CONTENT_FILE_BUILD_NO_AGP = """
            plugins {
                id("org.weblate.android")
            }
        """.trimIndent()

        private val CONTENT_FILE_BUILD_NO_CONFIG = """
            plugins {
                id("com.android.application")
                id("org.weblate.android")
            }

            android {
                namespace = "com.example.test"
                compileSdk = 37

                defaultConfig {
                    applicationId = "com.example.test"
                    minSdk = 21
                    targetSdk = 37
                    versionCode = 1
                    versionName = "1.0.0"
                }
            }
        """.trimIndent()

        private val CONTENT_FILE_BUILD_WITH_CONFIG = """
            plugins {
                id("com.android.application")
                id("org.weblate.android")
            }

            android {
                namespace = "com.example.test"
                compileSdk = 37

                defaultConfig {
                    applicationId = "com.example.test"
                    minSdk = 21
                    targetSdk = 37
                    versionCode = 1
                    versionName = "1.0.0"
                }
            }

            weblate {
                serverUrl = "https://weblate.example.com/"
                authToken = "AUTH_TOKEN"
                project = "example"
                component = "test"

                sdk {
                    cdnUrl = "https://cdn.example.com/"
                }
            }
        """.trimIndent()
    }
}
