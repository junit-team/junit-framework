plugins {
	id("junitbuild.java-library-conventions")
}

description = "JUnit Platform Configuration Processor"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformConfigurationApi)
	api(projects.junitPlatformCommons)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)
}
