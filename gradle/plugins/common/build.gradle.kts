import junitbuild.extensions.dependencyFromLibs
import junitbuild.extensions.markerCoordinates

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("junitbuild.base:dsl-extensions")
	implementation("junitbuild.base:build-parameters")
	implementation(projects.backwardCompatibility)
	implementation(libs.plugins.kotlin.markerCoordinates)
	implementation(libs.plugins.bnd.markerCoordinates)
	implementation(libs.plugins.develocity.markerCoordinates)
	implementation(libs.plugins.errorProne.markerCoordinates)
	implementation(libs.plugins.jmh.markerCoordinates)
	implementation(libs.plugins.nullaway.markerCoordinates)
	implementation(libs.plugins.shadow.markerCoordinates)
	implementation(libs.plugins.spotless.markerCoordinates)
	implementation(platform(dependencyFromLibs("log4j-bom"))) {
		because("Workaround for CVE-2025-68161")
	}
	constraints {
		implementation("org.codehaus.plexus:plexus-utils") {
			version {
				require("4.0.3")
			}
			because("Workaround for CVE-2025-67030 (used by shadow plugin)")
		}
	}
}
