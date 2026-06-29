import junitbuild.extensions.markerCoordinates

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("junitbuild.base:dsl-extensions")
	implementation(projects.buildParameters)
	implementation(libs.plugins.nmcp.settings.markerCoordinates)
}
