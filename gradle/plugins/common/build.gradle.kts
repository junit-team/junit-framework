import junitbuild.extensions.markerCoordinates

plugins {
	`kotlin-dsl`
}

dependencies {
	implementation("junitbuild.base:dsl-extensions")
	implementation(projects.buildParameters)
	implementation(libs.plugins.kotlin.markerCoordinates)
	implementation(libs.plugins.bnd.markerCoordinates)
	implementation(libs.plugins.commonCustomUserData.markerCoordinates)
	implementation(libs.plugins.develocity.markerCoordinates)
	implementation(libs.plugins.foojayResolver.markerCoordinates)
	implementation(libs.plugins.jmh.markerCoordinates)
	implementation(libs.plugins.jreleaser.markerCoordinates)
	implementation(libs.plugins.shadow.markerCoordinates)
	implementation(libs.plugins.spotless.markerCoordinates)
	implementation(libs.plugins.versions.markerCoordinates)
}
