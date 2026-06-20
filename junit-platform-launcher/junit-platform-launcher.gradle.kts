plugins {
	id("junitbuild.java-library-conventions")
	`java-test-fixtures`
}

description = "JUnit Platform Launcher"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformEngine)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	osgiVerification(projects.junitJupiterEngine)
}

javadocConventions {
	addExtraModuleReferences(projects.junitPlatformReporting)
}

tasks {
	jar {
		bundle {
			bnd("""
				Import-Package: \
					${extra["importAPIGuardian"]},\
					${extra["importJSpecify"]},\
					${extra["importCommonsLogging"]},\
					jdk.jfr;resolution:="optional",\
					*
				Provide-Capability:\
					org.junit.platform.launcher;\
						org.junit.platform.launcher='junit-platform-launcher';\
						version:Version="${'$'}{version_cleanup;${project.version}}"
			""")
		}
	}
}
