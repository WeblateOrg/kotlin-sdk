/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalAbiValidation::class)

import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

val signingKeyId: String? = System.getenv("PGP_SIGNING_KEY_ID")
val signingKey: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY")
val signingPassword: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY_PASSWORD")

plugins {
    alias(libs.plugins.android.library.core)
    alias(libs.plugins.jetbrains.dokka.html)
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
        minSdk = 21
        aarMetadata {
            minCompileSdk = 21
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.json)
    implementation(libs.androidx.annotations)
    implementation(libs.jetbrains.kotlinx.coroutines)
}

// Fixes warning about API being provided by Android
configurations {
    all {
        exclude(group = "org.json", module = "json")
    }
}

dokka {
    pluginsConfiguration.html {
        customStyleSheets.from(layout.projectDirectory.file("../docs/dokka/weblate.css"))
        customAssets.from(
            layout.projectDirectory.file("../docs/dokka/Logo-Darktext.svg"),
            layout.projectDirectory.file("../docs/dokka/Logo-Whitetext.svg"),
            layout.projectDirectory.file("../docs/dokka/logo-icon.svg"),
        )
    }
}

publishing {
    publications {
        val mavenGroupId = "org.weblate"
        val mavenArtifactId = "android"
        fun MavenPublication.setupPom() = pom {
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

        register<MavenPublication>("release") {
            group = mavenGroupId
            artifactId = mavenArtifactId
            version = libs.versions.weblate.get()

            afterEvaluate {
                from(components["release"])
            }

            setupPom()
        }

        register<MavenPublication>("snapshot") {
            group = mavenGroupId
            artifactId = mavenArtifactId
            version = libs.versions.weblate.get() + "-SNAPSHOT"

            afterEvaluate {
                from(components["release"])
            }

            setupPom()
        }

        repositories {
            maven {
                name = "centralSnapshot"
                url = uri("https://central.sonatype.com/repository/maven-snapshots/")
                credentials {
                    username = System.getenv("SONATYPE_MAVEN_CENTRAL_USERNAME")
                    password = System.getenv("SONATYPE_MAVEN_CENTRAL_PASSWORD")
                }
            }
            maven {
                name = "centralRelease"
                url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("SONATYPE_MAVEN_CENTRAL_USERNAME")
                    password = System.getenv("SONATYPE_MAVEN_CENTRAL_PASSWORD")
                }
            }
        }
    }
}

signing {
    isRequired = !signingKey.isNullOrEmpty()
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}
