plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.junit4-compatibility")
}

description = "JUnit Jupiter Migration Support"

dependencies {
	api(platform(projects.junitBom))
	api(libs.junit4)
	api(projects.junitJupiterApi)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	osgiVerification(projects.junitJupiterEngine)
	osgiVerification(projects.junitPlatformLauncher)
}

tasks {
	compileJava {
		options.compilerArgs.add("-Xlint:-requires-automatic,-requires-transitive-automatic") // JUnit 4
	}
	jar {
		bundle {
			bnd("""
				# Import JUnit4 packages with a version
				Import-Package: \
					${extra["importAPIGuardian"]},\
					${extra["importJSpecify"]},\
					${extra["importCommonsLogging"]},\
					org.junit;version="[${libs.versions.junit4Min.get()},5)",\
					org.junit.rules;version="[${libs.versions.junit4Min.get()},5)",\
					*
			""")
		}
	}
}
