import junitbuild.java.UpdateJarAction

plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.java-nullability-conventions")
}

description = "JUnit On-Ramp Module"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitJupiter)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)
	compileOnlyApi(projects.junitJupiterEngine)

	implementation(projects.junitPlatformLauncher)
	implementation(projects.junitPlatformConsole)
}

japicmp {
	enabled = false // no previous version, yet
}
