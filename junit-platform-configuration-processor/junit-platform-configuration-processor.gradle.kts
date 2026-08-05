plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.shadow-conventions")
}

description = "JUnit Platform Configuration Processor"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformConfigurationApi)
	api(projects.junitPlatformCommons)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	// TODO: Shade, but it's non trivial.
	implementation(libs.jackson.databind)
}

backwardCompatibilityChecks {
	enabled = false // not yet released
}
