package junitbuild.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.file.FileTreeElement
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import javax.inject.Inject

/**
 * Merges JUnit/Spring configuration metadata files by concatenating the array
 * values of their top-level fields (`groups`, `properties`, and `hints`).
 *
 * @see <a href="https://docs.spring.io/spring-boot/specification/configuration-metadata/format.html">Configuration Metadata format</a>
 */
abstract class ConfigurationMetadataMergingTransformer @Inject constructor(
    override val objectFactory: ObjectFactory
) : ResourceTransformer {

    @get:Input
    abstract val resource: Property<String>

    private var mergedTree: ObjectNode? = null

    override fun canTransformResource(element: FileTreeElement): Boolean {
        return resource.get().equals(element.path, ignoreCase = true)
    }

    override fun transform(context: TransformerContext) {
        val tree = JsonMapper().readTree(context.inputStream) as ObjectNode
        val merged = mergedTree
        if (merged == null) {
            mergedTree = tree
        } else {
            tree.properties().forEach { (name, value) ->
                value as ArrayNode
                val existing = merged.get(name)
                if (existing == null) {
                    merged.set(name, value)
                } else {
                    existing as ArrayNode
                    existing.addAll(value)
                }
            }
        }
    }

    override fun hasTransformedResource(): Boolean = mergedTree != null

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        val entry = ZipEntry(resource.get())
        entry.time = ShadowJar.CONSTANT_TIME_FOR_ZIP_ENTRIES
        os.putNextEntry(entry)
        os.write(JsonMapper().writeValueAsBytes(mergedTree))
        os.closeEntry()
        mergedTree = null
    }

}
