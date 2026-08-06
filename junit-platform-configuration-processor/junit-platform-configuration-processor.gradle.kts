plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.shadow-conventions")
}

description = "JUnit Platform Configuration Processor"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformConfigurationApi)
	api(projects.junitPlatformCommons)
	// TODO: Shade?
	api(libs.jakarta.json.api)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	// TODO: Check if shadowed artifact is standalone
	// TODO: Shade, but it's non trivial.
	implementation(libs.jakarta.json.implementation)
}

backwardCompatibilityChecks {
	enabled = false // not yet released
}
