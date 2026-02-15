import junitbuild.generator.GenerateJreRelatedSourceCode

plugins {
	id("junitbuild.kotlin-library-conventions")
	id("junitbuild.code-generator")
	`java-test-fixtures`
}

description = "JUnit Jupiter API"

dependencies {
	api(platform(projects.junitBom))
	api(libs.opentest4j)
	api(projects.junitPlatformCommons)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	compileOnly(kotlin("stdlib"))

	testFixturesImplementation(libs.assertj)
	testFixturesImplementation(testFixtures(projects.junitPlatformCommons))

	osgiVerification(projects.junitJupiterEngine)
	osgiVerification(projects.junitPlatformLauncher)
}

javadocConventions {
	addExtraModuleReferences(projects.junitPlatformEngine, projects.junitPlatformLauncher, projects.junitJupiterParams)
}

eclipseConventions {
	hideModularity = false
}

tasks {
	compileJava {
		options.compilerArgs.add("-Xlint:-module") // due to qualified exports
	}
	jar {
		bundle {
			val version = project.version
			bnd("""
				Require-Capability:\
					org.junit.platform.engine;\
						filter:='(&(org.junit.platform.engine=junit-jupiter)(version>=${'$'}{version_cleanup;${version}})(!(version>=${'$'}{versionmask;+;${'$'}{version_cleanup;${version}}})))';\
						effective:=active
			""")
		}
	}
	val generateJreTestDouble by registering(GenerateJreRelatedSourceCode::class) {
		templateDir = layout.projectDirectory.dir("src/templates/resources/main")
		targetDir = layout.buildDirectory.dir("generated/sources/jte/testDouble")
		maxVersion = 22
		fileNamePrefix = "TestDouble"
		additionalTemplateParameters = mapOf("classNamePrefix" to "TestDouble")
	}
	sourceSets.testFixtures.get().java.srcDir(generateJreTestDouble.map { it.targetDir })
}
