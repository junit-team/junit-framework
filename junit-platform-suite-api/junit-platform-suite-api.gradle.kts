plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.java-nullability-conventions")
}

description = "JUnit Platform Suite API"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformCommons)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	osgiVerification(projects.junitJupiterEngine)
	osgiVerification(projects.junitPlatformLauncher)
}

javadocConventions {
	addExtraModuleReferences(projects.junitPlatformEngine, projects.junitPlatformLauncher)
}
