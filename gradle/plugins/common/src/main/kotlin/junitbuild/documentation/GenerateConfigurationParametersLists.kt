package junitbuild.documentation

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import javax.inject.Inject

@CacheableTask
abstract class GenerateConfigurationParametersLists @Inject constructor(
    private val archives: ArchiveOperations
) : DefaultTask() {

    @get:Classpath
    abstract val metadataJars: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val mapper = JsonMapper()

        val propertiesByGroup = GROUPS.associate { it.title to mutableListOf<JsonNode>() }
        metadataJars.forEach { jar ->
            val group = GROUPS.firstOrNull { jar.name.startsWith(it.prefix) } ?: return@forEach
            archives.zipTree(jar).matching { include(METADATA_PATH) }.forEach { file ->
                val properties = mapper.readTree(file).path("properties")
                properties.forEach { propertiesByGroup.getValue(group.title).add(it) }
            }
        }

        val sb = StringBuilder()
        propertiesByGroup.forEach { (title, properties) ->
            if (properties.isEmpty()) {
                return@forEach
            }
            sb.appendLine("[[configuration-parameters-${title.lowercase().replace(' ', '-')}]]")
            sb.appendLine("=== $title")
            sb.appendLine()
            properties.sortedBy { it.path("name").asString() }.forEach { property ->
                val name = property.path("name").asString()
                sb.appendLine("`$name`::")
                val description = text(property.path("description").asString(""))
                val default = property.path("defaultValue")
                val hasDefault = !default.isMissingNode
                if (description.isNotEmpty()) {
                    val terminated = if (hasDefault && !description.endsWith(".")) "$description." else description
                    sb.appendLine("  $terminated")
                }
                if (hasDefault) {
                    sb.appendLine("  Defaults to `${text(default.asString())}`.")
                }
                if (property.has("deprecation")) {
                    sb.appendLine("  _(deprecated)_")
                }
                sb.appendLine()
            }
        }

        outputFile.get().asFile.writeText(sb.toString())
    }

    private fun text(value: String) = value.replace('\n', ' ').trim()

    private data class Group(val title: String, val prefix: String)

    companion object {
        private const val METADATA_PATH = "META-INF/junit-platform-configuration-metadata.json"

        private val GROUPS = listOf(
            Group("JUnit Platform", "junit-platform"),
            Group("JUnit Jupiter", "junit-jupiter"),
            Group("JUnit Vintage", "junit-vintage"),
        )
    }
}
