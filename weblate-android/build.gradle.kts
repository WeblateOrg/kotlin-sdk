/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

import com.android.build.api.dsl.LibraryExtension
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.io.encoding.Base64
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val signingKeyId: String? = System.getenv("PGP_SIGNING_KEY_ID")
val signingKey: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY")
val signingPassword: String? = System.getenv("PGP_PRIVATE_SIGNING_KEY_PASSWORD")

val mavenCentralUserName: String? = System.getenv("SONATYPE_MAVEN_CENTRAL_USERNAME")
val mavenCentralPassword: String? = System.getenv("SONATYPE_MAVEN_CENTRAL_PASSWORD")

val mavenGroupId = "org.weblate"
val mavenArtifactId = "android"

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
    dokkaPublications.html {
        moduleName.set("Kotlin SDK for Weblate")
    }

    dokkaSourceSets.configureEach {
        includes.from(layout.projectDirectory.file("../README.md"))
    }

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
                    username = mavenCentralUserName
                    password = mavenCentralPassword
                }
            }
            maven {
                name = "centralRelease"
                url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                credentials {
                    username = mavenCentralUserName
                    password = mavenCentralPassword
                }
            }
        }
    }
}

// https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuring-the-repository
tasks.register("notifyCentralReleaseRepository") {
    mustRunAfter("publishReleasePublicationToCentralReleaseRepository")
    group = "publishing"
    description = "Notifies central repository that a new release was published using staging API"

    doLast {
        val url = "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/"
        val authToken = Base64.encode(
            "$mavenCentralUserName:$mavenCentralPassword".encodeToByteArray()
        )
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$url$mavenGroupId?publishing_type=automatic"))
            .header("Authorization", "Bearer $authToken")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

        val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .join()
        when (response.statusCode()) {
            in 200..299 -> logger.warn("Successfully uploaded release to central repository")
            else -> throw Exception("[${response.statusCode()}]: ${response.body()}")
        }
    }
}

signing {
    isRequired = !signingKey.isNullOrEmpty()
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}
