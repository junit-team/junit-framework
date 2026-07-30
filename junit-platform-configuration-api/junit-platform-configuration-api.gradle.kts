plugins {
	id("junitbuild.java-library-conventions")
}

description = "JUnit Platform Configuration API"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformCommons)

	compileOnlyApi(libs.jspecify)
}
