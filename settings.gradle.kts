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

includeBuild("gradle/base")

rootProject.name = "junit-framework"

include("documentation")
include("junit-jupiter")
include("junit-jupiter-api")
include("junit-jupiter-engine")
include("junit-jupiter-migrationsupport")
include("junit-jupiter-params")
include("junit-start")
include("junit-platform-commons")
include("junit-platform-console")
include("junit-platform-console-standalone")
include("junit-platform-engine")
include("junit-platform-launcher")
include("junit-platform-reporting")
include("junit-platform-suite")
include("junit-platform-suite-api")
include("junit-platform-suite-engine")
include("junit-platform-testkit")
include("junit-vintage-engine")
include("jupiter-tests")
include("platform-tests")
include("platform-tooling-support-tests")
include("junit-bom")

// check that every subproject has a custom build file
// based on the project name
rootProject.children.forEach { project ->
	project.buildFileName = "${project.name}.gradle.kts"
	require(project.buildFile.isFile) {
		"${project.buildFile} must exist"
	}
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS")
enableFeaturePreview("ENHANCED_GRAPH_ORDERING")
