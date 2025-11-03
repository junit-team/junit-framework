package junitbuild.modules

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.util.PropertiesUtils.store
import java.io.File
import java.lang.module.ModuleFinder
import java.util.*

@CacheableTask
abstract class WriteModuleArtifactMapping : DefaultTask() {

    @get:Nested
    abstract val modularDependencies: SetProperty<ModularDependency>

    @get:OutputFile
    abstract val propertiesFile: RegularFileProperty

    fun from(configuration: Provider<out Configuration>) {
        modularDependencies.addAll(configuration.map {
            it.resolvedConfiguration.resolvedArtifacts
                .map { artifact ->
                    artifact.moduleVersion.id.let { id ->
                        ModularDependency(id.group, id.name, id.version, artifact.file)
                    }
                }
        })
    }

    @TaskAction
    fun run() {
        val properties = Properties()
        modularDependencies.get().forEach { dependency ->
            ModuleFinder.of(dependency.file.toPath())
                .findAll()
                .filter { !it.descriptor().isAutomatic }
                .forEach { module ->
                    properties.setProperty(
                        module.descriptor().name(),
                        "pkg:maven/${dependency.groupId}/${dependency.artifactId}@${dependency.version}"
                    )
                }
        }
        store(properties, propertiesFile.get().asFile)
    }

    data class ModularDependency(
        @get:Input val groupId: String,
        @get:Input val artifactId: String,
        @get:Input val version: String,
        @get:Classpath val file: File
    )

}
