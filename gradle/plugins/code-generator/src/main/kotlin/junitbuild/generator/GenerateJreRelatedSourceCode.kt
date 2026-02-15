package junitbuild.generator

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.FileOutput
import gg.jte.resolve.DirectoryCodeResolver
import junitbuild.generator.model.JRE
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import tools.jackson.core.type.TypeReference
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.KotlinModule

@CacheableTask
abstract class GenerateJreRelatedSourceCode : DefaultTask() {

    @get:InputDirectory
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateDir: DirectoryProperty

    @get:OutputDirectory
    abstract val targetDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val licenseHeaderFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val maxVersion: Property<Int>

    @get:Input
    @get:Optional
    abstract val fileNamePrefix: Property<String>

    @get:Input
    abstract val additionalTemplateParameters: MapProperty<String, String>

    @TaskAction
    fun generateSourceCode() {
        val mainTargetDir = targetDir.get().asFile
        mainTargetDir.deleteRecursively()

        val templateDir = templateDir.get().asFile
        val codeResolver = DirectoryCodeResolver(templateDir.toPath())
        val templateEngine =
            TemplateEngine.create(codeResolver, temporaryDir.toPath(), ContentType.Plain, javaClass.classLoader)

        val templates = templateDir.walkTopDown()
            .filter { it.extension == "jte" }
            .map { it.relativeTo(templateDir) }
            .toList()

        if (templates.isNotEmpty()) {
            var jres = javaClass.getResourceAsStream("/jre.yaml").use { input ->
                val mapper = YAMLMapper.builder()
                    .addModule(KotlinModule.Builder().build())
                    .build()
                mapper.readValue(input, object : TypeReference<List<JRE>>() {})
            }
            if (maxVersion.isPresent) {
                jres = jres.filter { it.version <= maxVersion.get() }
            }
            val minRuntimeVersion = 17
            val supportedJres = jres.filter { it.version >= minRuntimeVersion }
            val params = additionalTemplateParameters.get() + mapOf(
                "minRuntimeVersion" to minRuntimeVersion,
                "allJres" to jres,
                "supportedJres" to supportedJres,
                "supportedJresSortedByStringValue" to supportedJres.sortedBy { it.version.toString() },
                "licenseHeader" to licenseHeaderFile.asFile.get().readText().trimEnd() + "\n",
            )
            templates.forEach {
                val fileName = "${fileNamePrefix.getOrElse("")}${it.nameWithoutExtension}"
                val targetFile = mainTargetDir.toPath().resolve(it.resolveSibling(fileName).path)

                FileOutput(targetFile).use { output ->
                    // JTE does not support Windows paths, so we need to replace them
                    val safePath = it.path.replace('\\', '/')
                    templateEngine.render(safePath, params, output)
                }
            }
        }
    }

}
