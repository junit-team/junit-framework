plugins {
	id("junitbuild.java-library-conventions")
}

description = "JUnit Start Module"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitJupiter)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)
	compileOnlyApi(projects.junitJupiterEngine)

	implementation(projects.junitPlatformLauncher)
	implementation(projects.junitPlatformConsole)
}

backwardCompatibilityChecks {
	enabled = false // TODO enable after initial release
}
