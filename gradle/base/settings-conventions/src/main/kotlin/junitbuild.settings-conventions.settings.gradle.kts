import buildparameters.BuildParametersExtension
import com.gradle.develocity.agent.gradle.DevelocityConfiguration

plugins {
	id("junitbuild.build-parameters")
	id("com.gradle.develocity")
	id("com.gradle.common-custom-user-data-gradle-plugin")
	id("org.gradle.toolchains.foojay-resolver-convention")
}

val buildParameters = the<BuildParametersExtension>()
val useDevelocityInstance = !gradle.startParameter.isBuildScan

develocity {
	if (useDevelocityInstance) {
		// Publish to scans.gradle.com when `--scan` is used explicitly
		server = "https://develocity.junit.org"
		edgeDiscovery = true
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
		remote(the<DevelocityConfiguration>().buildCache) {
			server = buildCacheServer.orNull
			isPush = buildParameters.junit.develocity.buildCache.pushEnabled
		}
	} else if (buildCacheServer.isPresent) {
		remote<HttpBuildCache> {
			url = uri(buildCacheServer.get()).resolve("/cache/")
		}
	}
}
