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
        val hintsByGroup = GROUPS.associate { it.title to mutableListOf<JsonNode>() }
        metadataJars.forEach { jar ->
            val group = GROUPS.firstOrNull { jar.name.startsWith(it.prefix) } ?: return@forEach
            archives.zipTree(jar).matching { include(METADATA_PATH) }.forEach { file ->
                val contents = mapper.readTree(file)
                val properties = contents.path("properties")
                properties.forEach { propertiesByGroup.getValue(group.title).add(it) }
                val hints = contents.path("hints")
                hints.forEach { hintsByGroup.getValue(group.title).add(it) }
            }
        }

        val sb = StringBuilder()
        propertiesByGroup.forEach { (title, properties) ->
            if (properties.isEmpty()) {
                return@forEach
            }
            val hints = hintsByGroup[title]

            sb.appendLine("[[configuration-parameters-${title.lowercase().replace(' ', '-')}]]")
            sb.appendLine("=== $title")
            sb.appendLine()
            properties.sortedBy { it.path("name").asString() }.forEach { property ->
                val name = property.path("name").asString()
                val deprecation = if (property.has("deprecation")) " _(deprecated)_" else ""
                sb.appendLine("==== `$name`$deprecation")

                val description = text(property.path("description").asString(""))
                val default = property.path("defaultValue")
                val hasDefault = !default.isMissingNode

                if (description.isNotEmpty()) {
                    sb.appendLine(description)
                }

                val hint = hints?.find { jsonNode ->  jsonNode.get("name")?.asString() == name}
                val classReferenceProvider = hint?.get("providers")?.find { provider -> provider.get("name")?.asString() == "class-reference" }
                if (classReferenceProvider != null) {
                    val valueMustBeSubtypeOf = classReferenceProvider.get("parameters").get("target").stringValue()
                    sb.appendLine("Value must be a class that implements `$valueMustBeSubtypeOf`.")
                }
                if (hasDefault) {
                    sb.appendLine("Defaults to `${text(default.asString())}`.")
                }

                val isBoolean = property.get("type")?.stringValue() == "java.lang.Boolean"
                if(hint != null && classReferenceProvider == null && !isBoolean){
                    val values = hint.get("values")
                    val anyProvider = hint.get("providers")?.find { provider -> provider.get("name")?.asString() == "any" }
                    val valueTitle = if (anyProvider != null) "Example values" else "Allowed values"

                    sb.appendLine(
                        """
                        [cols="1,1"]
                        |===
                        |$valueTitle |Description
                        """.trimIndent()
                    )
                    values.forEach({
                        val value = it.get("value")?.asString()
                        val description = it.get("description")?.asString() ?: ""
                        sb.appendLine("|`$value`")
                        sb.appendLine("|$description" )
                        sb.appendLine()
                    })
                    sb.appendLine("|===")
                    sb.appendLine()
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
