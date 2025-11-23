import buildparameters.BuildParametersExtension
import org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS

pluginManagement {
	includeBuild("gradle/plugins")
	repositories {
		gradlePluginPortal()
	}
}

plugins {
	id("junitbuild.build-parameters")
	id("junitbuild.settings-conventions")
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
	}
}

val buildParameters = the<BuildParametersExtension>()
val develocityServer = "https://ge.junit.org"
val useDevelocityInstance = !gradle.startParameter.isBuildScan

develocity {
	if (useDevelocityInstance) {
		// Publish to scans.gradle.com when `--scan` is used explicitly
		server = develocityServer
	}
	buildScan {
		uploadInBackground = !buildParameters.ci

		publishing {
			onlyIf { it.isAuthenticated }
		}

		obfuscation {
			if (buildParameters.ci) {
				username { "github" }
			} else {
				hostname { null }
				ipAddresses { emptyList() }
			}
		}

		if (buildParameters.junit.develocity.testDistribution.enabled) {
			tag("test-distribution")
		}
	}
}

buildCache {
	local {
		isEnabled = !buildParameters.ci
	}
	val buildCacheServer = buildParameters.junit.develocity.buildCache.server
	if (useDevelocityInstance) {
		remote(develocity.buildCache) {
			server = buildCacheServer.orNull
			val authenticated = !System.getenv("DEVELOCITY_ACCESS_KEY").isNullOrEmpty()
			isPush = buildParameters.ci && authenticated
		}
	} else {
		remote<HttpBuildCache> {
			url = uri(buildCacheServer.getOrElse(develocityServer)).resolve("/cache/")
		}
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
