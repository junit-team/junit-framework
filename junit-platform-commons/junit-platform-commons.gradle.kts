import junitbuild.extensions.javaModuleName

plugins {
	id("junitbuild.java-nullability-conventions")
	id("junitbuild.kotlin-library-conventions")
	`java-test-fixtures`
}

description = "JUnit Platform Commons"

dependencies {
	api(platform(projects.junitBom))

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	compileOnly(kotlin("stdlib"))
	compileOnly(kotlin("reflect"))
	compileOnly(libs.kotlinx.coroutines)

	testFixturesImplementation(libs.assertj)
}

javadocConventions {
	addExtraModuleReferences(projects.junitPlatformEngine)
}

eclipseConventions {
	hideModularity = false
}

tasks.compileJava {
	options.compilerArgs.add("-Xlint:-module") // due to qualified exports
	val moduleName = javaModuleName
	val mainOutput = files(sourceSets.main.get().output)
	options.compilerArgumentProviders.add(CommandLineArgumentProvider {
		listOf("--patch-module", "${moduleName}=${mainOutput.asPath}")
	})
}

tasks.jar {
	bundle {
		val importAPIGuardian: String by extra
		val importJSpecify: String by extra
		bnd("""
			Import-Package: \
				$importAPIGuardian,\
				$importJSpecify,\
				kotlin.*;resolution:="optional",\
				kotlinx.*;resolution:="optional",\
				*
		""")
	}
}
