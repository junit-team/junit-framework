plugins {
	id("junitbuild.java-library-conventions")
}

description = "JUnit Platform Configuration API"

dependencies {
	api(platform(projects.junitBom))

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)
}
