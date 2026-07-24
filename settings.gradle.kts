import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

pluginManagement {
	includeBuild("gradle/base")
	includeBuild("gradle/plugins")
	repositories {
		gradlePluginPortal()
	}
}

plugins {
	id("junitbuild.build-parameters")
	id("junitbuild.maven-central-publishing")
	id("junitbuild.settings-conventions")
	// Add the Kotlin plugin to the classpath to avoid classloader issues due
	// to included builds (see https://github.com/gradle/gradle/issues/31278).
	// Renovate will keep the version in sync with libs.versions.toml.
	id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
	}
}

rootProject.name = "junit-framework"

rootDir.toPath()
	.listDirectoryEntries()
	.asSequence()
	.filter { it.isDirectory() }
	.map { it.toFile() }
	.map { it to it.resolve("${it.name}.gradle.kts") }
	.filter { it.second.exists() }
	.forEach { (dir, buildScript) ->
		include(dir.name)
		project(dir).buildFileName = buildScript.name
	}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS")
enableFeaturePreview("ENHANCED_GRAPH_ORDERING")
