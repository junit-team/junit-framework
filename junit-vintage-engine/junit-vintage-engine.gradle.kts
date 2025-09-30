plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.java-nullability-conventions")
	id("junitbuild.junit4-compatibility")
	id("junitbuild.testing-conventions")
	`java-test-fixtures`
	groovy
}

description = "JUnit Vintage Engine"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformEngine)
	api(libs.junit4)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	testFixturesApi(platform(libs.groovy2.bom))
	testFixturesApi(libs.spock1)
	testFixturesImplementation(projects.junitPlatformSuiteApi)

	testImplementation(projects.junitPlatformLauncher)
	testImplementation(projects.junitPlatformSuiteEngine)
	testImplementation(projects.junitPlatformTestkit)
	testImplementation(testFixtures(projects.junitPlatformCommons))
	testImplementation(testFixtures(projects.junitJupiterApi))
	testImplementation(testFixtures(projects.junitPlatformLauncher))
	testImplementation(testFixtures(projects.junitPlatformReporting))

	osgiVerification(projects.junitPlatformLauncher)
}

tasks {
	compileJava {
		options.compilerArgs.add("-Xlint:-requires-automatic") // JUnit 4
	}
	compileTestFixturesGroovy {
		javaLauncher = project.javaToolchains.launcherFor {
			// Groovy 2.x (used for Spock tests) does not run on more recent JDKs
			languageVersion = JavaLanguageVersion.of(17)
		}
	}
	jar {
		bundle {
			val junit4Min = libs.versions.junit4Min.get()
			val version = project.version
			val importAPIGuardian: String by extra
			val importJSpecify: String by extra
			bnd("""
				# Import JUnit4 packages with a version
				Import-Package: \
					${importAPIGuardian},\
					${importJSpecify},\
					junit.runner;version="[${junit4Min},5)",\
					org.junit;version="[${junit4Min},5)",\
					org.junit.experimental.categories;version="[${junit4Min},5)",\
					org.junit.internal.builders;version="[${junit4Min},5)",\
					org.junit.platform.commons.logging;status=INTERNAL,\
					org.junit.runner.*;version="[${junit4Min},5)",\
					org.junit.runners.model;version="[${junit4Min},5)",\
					*

				Provide-Capability:\
					org.junit.platform.engine;\
						org.junit.platform.engine='junit-vintage';\
						version:Version="${'$'}{version_cleanup;$version}"
				Require-Capability:\
					org.junit.platform.launcher;\
						filter:='(&(org.junit.platform.launcher=junit-platform-launcher)(version>=${'$'}{version_cleanup;$version})(!(version>=${'$'}{versionmask;+;${'$'}{version_cleanup;$version}})))';\
						effective:=active
			""")
		}
	}
	val testWithoutJUnit4 by registering(Test::class) {
		val test by testing.suites.existing(JvmTestSuite::class)
		(options as JUnitPlatformOptions).apply {
			includeTags("missing-junit4")
		}
		setIncludes(listOf("**/JUnit4VersionCheckTests.class"))
		testClassesDirs = files(test.map { it.sources.output.classesDirs })
		classpath = files(test.map { it.sources.runtimeClasspath }).filter {
			!it.name.startsWith("junit-4")
		}
	}
	withType<Test>().named { it != testWithoutJUnit4.name }.configureEach {
		(options as JUnitPlatformOptions).apply {
			excludeTags("missing-junit4")
		}
	}
	withType<Test>().configureEach {
		// Workaround for Groovy 2.5
		jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
		jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
	}
	check {
		dependsOn(testWithoutJUnit4)
	}
}

eclipse {
	classpath {
		// Avoid exposing test resources to dependent projects
		containsTestFixtures = false
	}
	project {
		// Remove Groovy Nature, since we don't require a Groovy plugin for Eclipse
		// in order for developers to work with the code base.
		natures.removeAll { it == "org.eclipse.jdt.groovy.core.groovyNature" }
	}
}
