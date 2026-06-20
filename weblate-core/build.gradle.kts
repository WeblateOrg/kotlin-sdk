/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val signingKey: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY")
val signingPassword: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY_PASSWORD")
val shouldSignRelease: Boolean
    get() = !signingKey.isNullOrEmpty() && !signingPassword.isNullOrEmpty()

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.ktlint)
    `maven-publish`
    signing
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

kotlin {
    jvmToolchain(21)
    explicitApi = ExplicitApiMode.Strict

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes"
        )
    }

    abiValidation()

    android {
        namespace = "org.weblate.core"
        compileSdk {
            version = release(37) {
                minorApiLevel = 0
            }
        }
        minSdk {
            version = release(21)
        }
    }

    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.serialization.xml)
            implementation(libs.jetbrains.coroutines.core)
            implementation(libs.touchlab.kermit)
            api(libs.jetbrains.kotlin.io)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.jetbrains.coroutines.android)
            implementation(libs.androidx.startup.runtime)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.apache5)
        }
        commonTest.dependencies {
            implementation(libs.jetbrains.kotlin.test)
        }
    }
}

// Run "./gradlew publishAllPublicationToLocalRepository" to generate release JARs/klibs locally
publishing {
    publications {
        val artifactVersion = "1.0.0"
        group = "org.weblate"

        publications.withType<MavenPublication> {
            pom {
                name = "Weblate"
                description = "A library for syncing localizations directly into apps"
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

        repositories {
            maven {
                name = "centralSnapshot"
                version = "$artifactVersion-SNAPSHOT"
                url = uri("https://central.sonatype.com/repository/maven-snapshots/")
                credentials {
                    username = System.getenv("SONATYPE_MAVEN_CENTRAL_USERNAME")
                    password = System.getenv("SONATYPE_MAVEN_CENTRAL_PASSWORD")
                }
            }
            maven {
                name = "centralRelease"
                version = "$artifactVersion-alpha01"
                url = uri("https://central.sonatype.com/api/v1/publisher/deployments/download/")
                credentials {
                    username = System.getenv("SONATYPE_MAVEN_CENTRAL_USERNAME")
                    password = System.getenv("SONATYPE_MAVEN_CENTRAL_PASSWORD")
                }
            }
            maven {
                name = "local"
                url = uri(layout.buildDirectory.dir("maven"))
            }
        }
    }
}

signing {
    isRequired = shouldSignRelease
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
