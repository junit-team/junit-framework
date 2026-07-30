plugins {
	id("junitbuild.java-library-conventions")
	`java-test-fixtures`
}

description = "JUnit Platform Launcher"

dependencies {
	annotationProcessor(projects.junitPlatformConfigurationProcessor)

	api(platform(projects.junitBom))
	api(projects.junitPlatformEngine)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)
	compileOnlyApi(projects.junitPlatformConfigurationApi)

	osgiVerification(projects.junitJupiterEngine)
}

javadocConventions {
	addExtraModuleReferences(projects.junitPlatformConfigurationApi, projects.junitPlatformReporting)
}

tasks {
	compileJava {
		options.compilerArgs.add("-Xlint:-processing") // -processing: not all annotations need to be processed
	}
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
