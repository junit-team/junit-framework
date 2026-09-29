plugins {
	id("junitbuild.java-aggregator-conventions")
}

description = "JUnit Jupiter (Aggregator)"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitJupiterApi)
	api(projects.junitJupiterParams)

	implementation(projects.junitJupiterEngine)

	compileOnly(libs.apiguardian)
	compileOnly(libs.jspecify)

	osgiVerification(projects.junitJupiterEngine)
	osgiVerification(projects.junitPlatformLauncher)
}
