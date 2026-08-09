package junitbuild.extensions

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.the

val Project.javaModuleName: String
    get() = toModuleName(name)

val ProjectDependency.javaModuleName: String
    get() = toModuleName(name)

private fun toModuleName(name: String) = "org.${name.replace('-', '.')}"

val Project.artifactGroup: String
    get() = toArtifactGroup(name)

val ProjectDependency.artifactGroup: String
    get() = toArtifactGroup(name)

private fun toArtifactGroup(name: String) = when {
    name.startsWith("junit-jupiter") -> "org.junit.jupiter"
    name.startsWith("junit-platform") -> "org.junit.platform"
    name.startsWith("junit-vintage") -> "org.junit.vintage"
    else -> "org.junit"
}

@Suppress("UNCHECKED_CAST")
private val Project.mavenizedProjectPaths: List<String>
    get() = extra["mavenizedProjectPaths"] as List<String>

@Suppress("UNCHECKED_CAST")
private val Project.modularProjectPaths: List<String>
    get() = extra["modularProjectPaths"] as List<String>

val Project.mavenizedProjects: List<ProjectDependency>
    get() = mavenizedProjectPaths.map { dependencies.project(it) }

val Project.modularProjects: List<ProjectDependency>
    get() = modularProjectPaths.map { dependencies.project(it) }

val Project.isMavenized: Boolean
    get() = path in mavenizedProjectPaths

fun Project.requiredVersionFromLibs(name: String) =
    libsVersionCatalog.findVersion(name).get().requiredVersion

fun Project.dependencyFromLibs(name: String) =
    libsVersionCatalog.findLibrary(name).get()

fun Project.bundleFromLibs(name: String) =
    libsVersionCatalog.findBundle(name).get()

private val Project.libsVersionCatalog: VersionCatalog
    get() = the<VersionCatalogsExtension>().named("libs")
