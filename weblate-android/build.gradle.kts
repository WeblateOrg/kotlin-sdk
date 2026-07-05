/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val signingKey: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY")
val signingPassword: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY_PASSWORD")
val shouldSignRelease: Boolean
    get() = !signingKey.isNullOrEmpty() && !signingPassword.isNullOrEmpty()

plugins {
    alias(libs.plugins.android.library.core)
    `maven-publish`
    signing
}

kotlin {
    jvmToolchain(21)

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
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.weblateCore)
    implementation(libs.androidx.core)
}

// Run "./gradlew publishAllPublicationToLocalRepository" to generate release JARs/klibs locally
publishing {
    publications {
        val artifactVersion = "1.0.0"
        group = "org.weblate"

        publications.withType<MavenPublication> {
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
