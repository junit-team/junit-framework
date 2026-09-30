import junitbuild.extensions.markerCoordinates

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("junitbuild.base:build-parameters")
	implementation(libs.plugins.node.markerCoordinates)
	constraints {
		implementation("com.fasterxml.jackson.core:jackson-databind") {
			version {
				require("2.22.3")
			}
			because("Workaround for CVE-2026-68497")
		}
	}
	implementation(libs.plugins.spring.antora.markerCoordinates)
	constraints {
		implementation("org.yaml:snakeyaml") {
			version {
				require("2.0")
			}
			because("Workaround for CVE-2022-1471")
		}
	}
}
