plugins {
	id("junitbuild.java-library-conventions")
}

description = "JUnit Platform Configuration API"

dependencies {
	api(platform(projects.junitBom))

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)
}

backwardCompatibilityChecks {
	enabled = false // not yet released
}

tasks.jar {
	bundle {
		bnd(
			"""
			Import-Package: \
				${extra["importAPIGuardian"]},\
				${extra["importJSpecify"]},\
				*
			"""
		)
	}
}
