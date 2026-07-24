package junitbuild.javadoc

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.jvm.JavaModuleDetector
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavadocTool
import org.gradle.kotlin.dsl.the
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import javax.inject.Inject

@CacheableTask
abstract class GenerateJavadoc @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
    private val javaModuleDetector: JavaModuleDetector,
    javaToolchainService: JavaToolchainService
) : DefaultTask() {

    /**
     * Options and their values, one token per entry; written to an `@` options file.
     * If used, `-locale` must be the first option.
     */
    @get:Input
    abstract val args: ListProperty<String>

    @get:Nested
    val argumentProviders = mutableListOf<CommandLineArgumentProvider>()

    /**
     * Passed as `-J<flag>` on the command line because JVM flags are not allowed in the options file.
     */
    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Nested
    abstract val javadocTool: Property<JavadocTool>

    init {
        javadocTool.convention(javaToolchainService.javadocToolFor(project.the<JavaPluginExtension>().toolchain))
    }

    @TaskAction
    fun generate() {
        val outputDir = outputDirectory.get().asFile
        fileSystemOperations.delete { delete(outputDir) }
        outputDir.mkdirs()

        val optionsFile = temporaryDir.resolve("javadoc.options")
        optionsFile.printWriter().use { writer ->

            (args.get() + argumentProviders.flatMap { it.asArguments() })
                .forEach { writer.println(quote(it)) }

            writer.println("-d ${quote(outputDir.absolutePath)}")

            // Same classpath/module path split as Gradle's built-in Javadoc task
            val plainClasspath = javaModuleDetector.inferClasspath(true, classpath)
            val modulePath = javaModuleDetector.inferModulePath(true, classpath)
            if (!plainClasspath.isEmpty) {
                writer.println("-classpath ${quote(plainClasspath.asPath)}")
            }
            if (!modulePath.isEmpty) {
                writer.println("--module-path ${quote(modulePath.asPath)}")
            }

            sourceFiles.forEach { writer.println(quote(it.absolutePath)) }
        }

        try {
            execOperations.exec {
                executable = javadocTool.get().executablePath.asFile.absolutePath
                jvmArgs.get().forEach { args("-J$it") }
                args("@${optionsFile.absolutePath}")
            }
        } catch (e: Exception) {
            throw GradleException("Javadoc generation failed. Generated Javadoc options file (useful for troubleshooting): '$optionsFile'", e)
        }
    }

    private fun quote(token: String) =
        if (token.startsWith("-")) token
        else "'" + token.replace("\\", "\\\\").replace("'", "\\'") + "'"
}
