plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.java-nullability-conventions")
	`java-test-fixtures`
}

description = "JUnit Platform Engine API"

dependencies {
	api(platform(projects.junitBom))
	api(libs.opentest4j)
	api(projects.junitPlatformCommons)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	testImplementation(libs.assertj)

	osgiVerification(projects.junitJupiterEngine)
	osgiVerification(projects.junitPlatformLauncher)
}

javadocConventions {
	addExtraModuleReferences(projects.junitPlatformLauncher)
}
