pluginManagement {
	includeBuild("../base")
}

plugins {
	id("junitbuild.dsl-extensions") apply false
}

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			from(files("../libs.versions.toml"))
		}
	}
	repositories {
		gradlePluginPortal()
	}
}

rootProject.name = "plugins"

include("antora")
include("backward-compatibility")
include("build-parameters")
include("common")
include("code-generator")
include("publishing")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
