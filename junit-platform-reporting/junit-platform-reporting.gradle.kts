import junitbuild.extensions.javaModuleName

plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.shadow-conventions")
	`java-test-fixtures`
}

description = "JUnit Platform Reporting"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformLauncher)

	implementation(libs.openTestReporting.tooling.spi)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	shadowed(libs.openTestReporting.events)

	osgiVerification(projects.junitJupiterEngine)
	osgiVerification(projects.junitPlatformLauncher)
	osgiVerification(libs.openTestReporting.tooling.spi)

	testFixturesApi(projects.junitJupiterApi)
}

tasks {
	shadowJar {
		listOf("events", "schema").forEach { name ->
			val packageName = "org.opentest4j.reporting.${name}"
			relocate(packageName, "org.junit.platform.reporting.shadow.${packageName}")
		}
		from(projectDir) {
			include("LICENSE-open-test-reporting.md")
			into("META-INF")
		}
	}
	compileJava {
		options.compilerArgs.addAll(listOf(
			"--add-modules", "org.opentest4j.reporting.events",
			"--add-reads", "${javaModuleName}=org.opentest4j.reporting.events"
		))
	}
	javadoc {
		(options as StandardJavadocDocletOptions).apply {
			addStringOption("-add-modules", "org.opentest4j.reporting.events")
			addStringOption("-add-reads", "${javaModuleName}=org.opentest4j.reporting.events")
		}
	}
}
