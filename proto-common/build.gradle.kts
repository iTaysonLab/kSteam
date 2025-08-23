import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toOkioPath
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.incremental.deleteDirectoryContents

plugins {
    id("build-extensions")
    kotlin("multiplatform")
    id("com.android.library")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r48"

kotlin {
    multiplatformSetup()
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "client"
        nameSuffix = "Service"


    }
}

dependencies {
    commonMainApi(libs.wire.grpc) {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
}

configurations.all {
    exclude(group = "com.squareup.okhttp3", module = "okhttp")
}

androidLibrary("bruhcollective.itaysonlab.ksteam.proto")

//

abstract class UpgradeProtoFilesTask: DefaultTask() {
    @TaskAction
    fun action() {
        val kmpExt = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        val commonSrcDir = kmpExt.sourceSets.getByName("commonMain").kotlin.srcDirs.firstOrNull { d ->
            File(d.parentFile, "proto").exists()
        } ?: error("Could not find the source directory in commonMain: it must contain a `proto` folder in the source set near `kotlin`.")

        val protoInputDir = File(project.projectDir, "Protobufs")
        val protoOutputDir = File(commonSrcDir.parentFile, "proto")

        println("Patching WebUI protobufs...")
        val protoInWebUI = File(protoInputDir, "webui")
        val protoOutWebUI = File(protoOutputDir, "webui")
        patchWebUI(protoInWebUI, protoOutWebUI)

        println("Patching source marker...")
        patchSourceMarker(commonSrcDir)

        println("Done!")
    }

    companion object {
        private val sourceMarkerRegex = "const val GIT_HASH = \".*\"".toRegex(RegexOption.MULTILINE)
    }

    private fun patchSourceMarker(dir: File) {
        val path = dir.toOkioPath() / "bruhcollective" / "itaysonlab" / "ksteam" / "ProtoCommon.kt"

        require(FileSystem.SYSTEM.exists(path)) { "The ProtoCommon.kt must exist." }

        val hash = "git ls-tree --object-only HEAD Protobufs".runCommand(project.projectDir).orEmpty()
        println("-> $hash")

        val content = path.toFile().readText()
        FileSystem.SYSTEM.write(path) {
            writeUtf8(content.replace(sourceMarkerRegex, "const val GIT_HASH = \"${hash.take(8)}\""))
        }
    }

    private fun patchWebUI(inDir: File, outDir: File) {
        require(inDir.exists()) { "Input directory ${inDir.absolutePath} should exist." }
        require(outDir.exists()) { "Output directory ${outDir.absolutePath} should exist." }

        // Cleanup
        outDir.deleteDirectoryContents()

        // 1. Add option java_package = "steam.webui.*service_name*"
        // 2. Fix imports: "common.proto" -> "webui/common.proto"
        for (file in inDir.walkTopDown().filter(File::isFile)) {
            println("-> ${file.name}")

            FileSystem.SYSTEM.write(outDir.toOkioPath() / file.name) {
                var writeOptionData = true
                var writeOptionNewline = false

                file.forEachLine { line ->
                    when {
                        line.startsWith("syntax") -> {
                            writeUtf8(line)
                            writeByte(0x0A)
                            writeOptionNewline = true
                            return@forEachLine
                        }

                        line.startsWith("import") -> {
                            writeUtf8(line.replace("common", "webui/common"))
                            writeByte(0x0A)
                            writeOptionNewline = true
                            return@forEachLine
                        }

                        else -> {
                            if (writeOptionData) {
                                if (writeOptionNewline) {
                                    writeByte(0x0A)
                                }

                                writeUtf8("option java_package = \"steam.webui.")

                                if (file.name.startsWith("common")) {
                                    writeUtf8("common")
                                } else {
                                    writeUtf8(file.nameWithoutExtension.removePrefix("service_"))
                                }

                                writeUtf8("\";\n")
                                writeOptionData = false
                            }

                            writeUtf8(line)
                            writeByte(0x0A) // newline
                        }
                    }
                }
            }
        }
    }

    // stolen from https://stackoverflow.com/a/41495542
    fun String.runCommand(workingDir: File): String? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            return proc.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}

tasks.register<UpgradeProtoFilesTask>("upgradeProtoFiles") {
    group = "ksteam"
    description = "Patches SteamDatabase submodule protobufs to Wire format"
}