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

            sb.appendLine("[[configuration-parameters-${title.lowercase().replace(' ', '-')}]]")
            sb.appendLine("=== $title")
            sb.appendLine()

            val hints = hintsByGroup[title]
            properties.sortedBy { it.path("name").asString() }
                .map { property ->
                    val propertyName = property.path("name").asString()
                    val hint = hints?.find { jsonNode -> jsonNode.get("name")?.asString() == propertyName }
                    renderProperty(propertyName, property, hint)
                }
                .forEach { property -> sb.append(property) }
        }

        outputFile.get().asFile.writeText(sb.toString())
    }

    private fun renderProperty(
        propertyName: String,
        property: JsonNode,
        hint: JsonNode?,
    ): StringBuilder {
        val sb = StringBuilder()

        // Render title + deprecation
        val deprecation = if (property.has("deprecation")) " _(deprecated)_" else ""
        sb.appendLine("==== `$propertyName`$deprecation")

        // Render description
        val description = property.path("description").asString(null)
        if (description != null) {
            sb.appendLine(text(description))
        }

        // Render class reference phrase
        val classReferenceProvider =
            hint?.get("providers")?.find { provider -> provider.get("name")?.asString() == "class-reference" }
        if (classReferenceProvider != null) {
            val valueMustBeSubtypeOf = classReferenceProvider.get("parameters").get("target").stringValue()
            sb.appendLine(
                "Value must be the fully qualified name of a class that implements ${
                    javaDocLink(
                        valueMustBeSubtypeOf,
                        true
                    )
                }."
            )
        }

        // Render hint table with default
        val hintValues = hint?.get("values")
        val default = property.path("defaultValue").asString(null)
        if (hintValues != null) {
            val anyProvider = hint.get("providers")?.find { provider -> provider.get("name")?.asString() == "any" }
            val hintsAreNonExhaustive = anyProvider != null
            sb.append(renderHintTableWithDefault(propertyName, hintValues, hintsAreNonExhaustive, default))
        }

        // Render default if there were no hints
        else if (default != null) {
            if (classReferenceProvider != null) {
                sb.appendLine("Defaults to `${javaDocLink(default)}`.")
            } else {
                sb.appendLine("Defaults to `${default}`.")
            }
        }
        sb.appendLine()

        return sb
    }

    private fun renderHintTableWithDefault(
        name: String,
        hintValues: JsonNode,
        hintsAreNonExhaustive: Boolean,
        default: String?
    ): StringBuilder {
        val sb = StringBuilder()
        val valueTitle = if (hintsAreNonExhaustive) "Example" else "Value"

        // Render table header
        if (default != null) {
            sb.appendLine(
                """
                [cols="1,1,98"]
                |===
                |Default |$valueTitle |Description
                """.trimIndent()
            )
        } else {
            sb.appendLine(
                """
                [cols="1,99"]
                |===
                |$valueTitle |Description
                """.trimIndent()
            )
        }

        // Render table body
        var foundDefaultInValues = false
        hintValues.forEach {
            val value = it.get("value")?.asString() ?: ""
            val description = it.get("description")?.asString() ?: ""
            if (default != null) {
                if (default.equals(value, true)) {
                    foundDefaultInValues = true
                    sb.appendLine("|✓")
                } else {
                    sb.appendLine("|")
                }
            }
            sb.appendLine("|`${escapeCellSeparator(value)}`")
            sb.appendLine("|${escapeCellSeparator(description)}")
            sb.appendLine()
        }
        sb.appendLine("|===")
        sb.appendLine()

        if (default != null && !foundDefaultInValues) {
            throw IllegalArgumentException("The default value of $name (${default}) must be one of the hint values.")
        }
        return sb
    }

    private fun text(value: String) = value.replace('\n', ' ').trim()

    private fun escapeCellSeparator(value: String) = value.replace("|", "{vbar}")

    private fun javaDocLink(fqn: String, useClassNameAsLinkText: Boolean = false): String {
        val (moduleName, packageName, className) = guessModulePackageAndClassName(fqn)
        val packagePath = packageName.replace(".", "/")
        val linkText = if (useClassNameAsLinkText) className else fqn
        return "{javadoc-root}/$moduleName/$packagePath/$className.html[$linkText]"
    }

    private fun guessModulePackageAndClassName(fqn: String): PackageClassName {
        // Class name probably starts at the first uppercased letter
        val guess = Regex("(.*?)\\.([A-Z].*)").find(fqn)
            ?: throw IllegalArgumentException("Could not guess package name for $fqn")
        val packageName = guess.groupValues[1]
        val className = guess.groupValues[2]

        // The module name is probably a well known suffix of the package name
        val moduleName = if (fqn.startsWith("org.junit.jupiter.api")) {
            "org.junit.jupiter.api"
        } else if (fqn.startsWith("org.junit.platform.engine")) {
            "org.junit.platform.engine"
        } else {
            throw IllegalArgumentException("Could not guess module name for $fqn")
        }

        return PackageClassName(moduleName, packageName, className)
    }

    private data class PackageClassName(val moduleName: String, val packageName: String, val className: String)

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
