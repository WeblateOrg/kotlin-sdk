/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:OptIn(ExperimentalAbiValidation::class)

import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

val signingKey: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY")
val signingPassword: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY_PASSWORD")
val shouldSignRelease: Boolean
    get() = !signingKey.isNullOrEmpty() && !signingPassword.isNullOrEmpty()

plugins {
    alias(libs.plugins.android.library.core)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.dokka.html)
    alias(libs.plugins.jetbrains.dokka.java)
    `maven-publish`
    signing
}

kotlin {
    jvmToolchain(21)

    explicitApi = ExplicitApiMode.Strict

    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

configure<LibraryExtension> {
    namespace = "org.weblate.android"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 30
        aarMetadata {
            minCompileSdk = 30
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core)

    implementation(libs.jetbrains.kotlin.serialization)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.okhttp)
}

// To generate documentation in Javadoc
val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        val artifactVersion = "1.0.0"

        register<MavenPublication>("release") {
            group = "org.weblate"
            artifactId = "android"
            version = artifactVersion

            afterEvaluate {
                from(components["release"])
            }

            artifact(dokkaJavadocJar)

            pom {
                name = "Weblate - Android"
                description = "An android library for syncing localizations directly into apps"
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
        }
    }
}

signing {
    isRequired = shouldSignRelease
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
