/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.plugin.android.task

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.json.JSONObject
import org.weblate.plugin.android.Constants.FILE_METADATA

/**
 * Task to generate metadata in JSON format to help Weblate server generate required files
 * to overlay on Android for updating translations on the go.
 */
internal abstract class GenerateMetadataTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val rFile: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val versionCodes: ListProperty<Int>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generateMetadata() {
        val rFile = rFile.get().asFile
        val metadataDir = outputDir.get().asFile
        val resources = parseResourceIdsFromRTxt(rFile)

        versionCodes.get().forEach { versionCode ->
            val versionDir = metadataDir.resolve(versionCode.toString()).apply { mkdirs() }
            val outputJsonFile = versionDir.resolve(FILE_METADATA)

            val json = JSONObject()
                .put("packageName", packageName.get())
                .put("versionCode", versionCode)
                .put("strings", resources.getValue("strings"))
                .put("plurals", resources.getValue("plurals"))

            outputJsonFile.apply {
                parentFile.mkdirs()
                writeText(json.toString(2))
            }
        }
    }

    private fun parseResourceIdsFromRTxt(file: File): Map<String, Map<String, String>> {
        val stringMap = mutableMapOf<String, String>()
        val pluralMap = mutableMapOf<String, String>()

        file.useLines { lines ->
            lines.forEach { line ->
                val tokens = line.split(" ")
                if (tokens.size >= 4 && tokens[0] == "int") {
                    val resourceType = tokens[1]
                    val key = tokens[2]
                    val resourceId = tokens[3]

                    when (resourceType) {
                        "string" -> stringMap[key] = resourceId
                        "plurals" -> pluralMap[key] = resourceId
                    }
                }
            }
        }

        // There is nothing to translate, is this even possible?
        require(stringMap.isNotEmpty() || pluralMap.isNotEmpty()) {
            "Unable to find any string or plurals keys to put in JSON metadata"
        }

        return mapOf("strings" to stringMap, "plurals" to pluralMap)
    }

    companion object {
        const val TASK_DESCRIPTION = "Generates JSON metadata for Weblate"
    }
}
