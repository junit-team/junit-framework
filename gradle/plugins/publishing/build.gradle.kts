import junitbuild.extensions.markerCoordinates

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("junitbuild.base:dsl-extensions")
	implementation("junitbuild.base:build-parameters")
	implementation(libs.plugins.nmcp.settings.markerCoordinates)
}
