package junitbuild.compatibility.roseau

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@CacheableTask
abstract class RoseauDiff : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Classpath
    abstract val toolClasspath: ConfigurableFileCollection

    @get:CompileClasspath
    abstract val libraryClasspath: ConfigurableFileCollection

    @get:CompileClasspath
    abstract val v1: RegularFileProperty

    @get:CompileClasspath
    abstract val v2: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val configFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val acceptedChangesCsvFile: RegularFileProperty

    @get:OutputDirectory
    abstract val reportDir: DirectoryProperty

    @TaskAction
    fun run() {
        val reportDir = reportDir.get().asFile.absoluteFile
        val reports = listOf(
            Report(reportDir.resolve("breaking-changes.html"), Report.Format.HTML),
            Report(reportDir.resolve("breaking-changes.csv"), Report.Format.CSV)
        )
        reports.forEach { report -> report.file.delete() }

        val effectiveConfigFile = writeEffectiveConfigFile(reports)

        val output = ByteArrayOutputStream()
        val result = execOperations.javaexec {
            mainClass = "io.github.alien.roseau.cli.RoseauCLI"
            classpath = toolClasspath
            args(
                "--classpath", libraryClasspath.asPath,
                "--v1", v1.get().asFile.absolutePath,
                "--v2", v2.get().asFile.absolutePath,
                "--diff",
                "--fail-on-bc",
                "--config", effectiveConfigFile.absolutePath,
            )
            if (acceptedChangesCsvFile.isPresent) {
                args("--ignored", acceptedChangesCsvFile.get().asFile.absolutePath)
            }
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            System.out.write(output.toByteArray())
            System.out.flush()
            reports.filter { it.file.exists() }.let { writtenReports ->
                if (writtenReports.isNotEmpty()) {
                    println("Reports:")
                    writtenReports.forEach {
                        println("- ${it.format.name}: ${it.file.toURI()}")
                    }
                }
            }
            if (result.exitValue == 1) {
                throw GradleException("Breaking API changes detected")
            }
            result.assertNormalExitValue()
        }
    }

    private fun writeEffectiveConfigFile(reports: List<Report>): File {
        val effectiveConfigFile = temporaryDir.resolve("roseau.yaml")
        configFile.get().asFile.copyTo(effectiveConfigFile, overwrite = true)
        FileOutputStream(effectiveConfigFile, true).bufferedWriter().use { writer ->
            val yamlFactory = YAMLFactory.builder().disable(WRITE_DOC_START_MARKER).build()
            val mapper = ObjectMapper(yamlFactory)
            mapper.writeValue(
                writer, mapOf(
                    "reports" to reports
                )
            )
        }
        return effectiveConfigFile
    }

    private data class Report(val file: File, val format: Format) {
        enum class Format {
            HTML, CSV
        }
    }

}
