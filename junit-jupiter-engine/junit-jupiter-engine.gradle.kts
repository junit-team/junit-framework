plugins {
	id("junitbuild.java-library-conventions")
	`java-test-fixtures`
}

description = "JUnit Jupiter Engine"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformEngine)
	api(projects.junitJupiterApi)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	osgiVerification(projects.junitPlatformLauncher)
}

tasks {
	jar {
		bundle {
			bnd("""
				Provide-Capability:\
					org.junit.platform.engine;\
						org.junit.platform.engine='junit-jupiter';\
						version:Version="${'$'}{version_cleanup;${project.version}}"
				Require-Capability:\
					org.junit.platform.launcher;\
						filter:='(&(org.junit.platform.launcher=junit-platform-launcher)(version>=${'$'}{version_cleanup;${project.version}})(!(version>=${'$'}{versionmask;+;${'$'}{version_cleanup;${project.version}}})))';\
						effective:=active
			""")
		}
	}
}
