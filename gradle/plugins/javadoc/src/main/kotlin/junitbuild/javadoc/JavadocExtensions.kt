package junitbuild.javadoc

import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.process.CommandLineArgumentProvider

fun GenerateJavadoc.linkOffline(baseUrl: String, elementListDir: Provider<Directory>) {
	argumentProviders += LinkOfflineArgumentProvider(baseUrl, elementListDir)
}

fun GenerateJavadoc.moduleSourcePath(moduleName: String, sourceDirectories: FileCollection) {
	argumentProviders += ModuleSourcePathArgumentProvider(moduleName, sourceDirectories)
}

fun GenerateJavadoc.since(file: Provider<RegularFile>) {
	argumentProviders += SinceValuesArgumentProvider(file)
}

fun GenerateJavadoc.overview(file: RegularFile) {
	argumentProviders += FileArgumentProvider("-overview", file)
}

fun GenerateJavadoc.addStylesheet(file: RegularFile) {
	argumentProviders += FileArgumentProvider("--add-stylesheet", file)
}

internal class LinkOfflineArgumentProvider(
	@get:Input val baseUrl: String,
	@get:InputDirectory @get:PathSensitive(NONE) val elementListDir: Provider<Directory>
) : CommandLineArgumentProvider {
	override fun asArguments() = listOf("-linkoffline", baseUrl, elementListDir.get().asFile.absolutePath)
}

internal class ModuleSourcePathArgumentProvider(
	@get:Input val moduleName: String,
	@get:InputFiles @get:PathSensitive(RELATIVE) val sourceDirectories: FileCollection
) : CommandLineArgumentProvider {
	override fun asArguments() = listOf(
		"--module-source-path",
		"$moduleName=${sourceDirectories.filter { it.exists() }.asPath}"
	)
}

internal class SinceValuesArgumentProvider(
	@get:InputFile @get:PathSensitive(NONE) val file: Provider<RegularFile>
) : CommandLineArgumentProvider {
	override fun asArguments() = listOf(
		"--since",
		file.get().asFile.readLines().filter { it.isNotBlank() }.joinToString(",")
	)
}

internal class FileArgumentProvider(
	@get:Input val option: String,
	@get:InputFile @get:PathSensitive(NONE) val file: RegularFile
) : CommandLineArgumentProvider {
	override fun asArguments() = listOf(
		option,
		file.asFile.absolutePath
	)
}
