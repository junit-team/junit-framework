plugins {
	id("junitbuild.java-library-conventions")
}

description = "JUnit Platform Suite Engine"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformEngine)
	api(projects.junitPlatformSuiteApi)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	implementation(projects.junitPlatformLauncher)

	osgiVerification(projects.junitJupiterEngine)
	osgiVerification(projects.junitPlatformLauncher)
}
