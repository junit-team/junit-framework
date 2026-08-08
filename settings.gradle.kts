import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import org.gradle.kotlin.dsl.extra

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

run {
	val mavenizedProjectPaths = mutableListOf<String>()
	val modularProjectPaths = mutableListOf<String>()

	fun includeProject(name: String, mavenized: Boolean = false, modular: Boolean = false) {
		require(!modular || mavenized) {
			"A modular project must also be mavenized: $name"
		}
		include(name)
		project(rootDir.resolve(name)).buildFileName = "${name}.gradle.kts"
		if (mavenized) {
			mavenizedProjectPaths.add(":$name")
		}
		if (modular) {
			modularProjectPaths.add(":$name")
		}
	}

	includeProject("documentation")
	includeProject("junit-bom")
	includeProject("junit-jupiter", mavenized = true, modular = true)
	includeProject("junit-jupiter-api", mavenized = true, modular = true)
	includeProject("junit-jupiter-engine", mavenized = true, modular = true)
	includeProject("junit-jupiter-migrationsupport", mavenized = true, modular = true)
	includeProject("junit-jupiter-params", mavenized = true, modular = true)
	includeProject("junit-platform-commons", mavenized = true, modular = true)
	includeProject("junit-platform-console", mavenized = true, modular = true)
	includeProject("junit-platform-console-standalone", mavenized = true)
	includeProject("junit-platform-engine", mavenized = true, modular = true)
	includeProject("junit-platform-launcher", mavenized = true, modular = true)
	includeProject("junit-platform-reporting", mavenized = true, modular = true)
	includeProject("junit-platform-suite", mavenized = true, modular = true)
	includeProject("junit-platform-suite-api", mavenized = true, modular = true)
	includeProject("junit-platform-suite-engine", mavenized = true, modular = true)
	includeProject("junit-platform-testkit", mavenized = true, modular = true)
	includeProject("junit-start", mavenized = true, modular = true)
	includeProject("junit-vintage-engine", mavenized = true, modular = true)
	includeProject("jupiter-tests")
	includeProject("platform-tests")
	includeProject("platform-tooling-support-tests")

	// Sort so the published order (e.g. BOM constraints) is independent of the include order above.
	mavenizedProjectPaths.sort()
	modularProjectPaths.sort()

	gradle.lifecycle.beforeProject {
		extra["mavenizedProjectPaths"] = mavenizedProjectPaths
		extra["modularProjectPaths"] = modularProjectPaths
	}
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS")
enableFeaturePreview("ENHANCED_GRAPH_ORDERING")
