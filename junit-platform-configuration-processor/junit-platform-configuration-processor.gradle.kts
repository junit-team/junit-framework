import junitbuild.extensions.javaModuleName

plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.shadow-conventions")
}

description = "JUnit Platform Configuration Processor"

dependencies {
	api(platform(projects.junitBom))
	api(projects.junitPlatformConfigurationApi)

	compileOnlyApi(libs.apiguardian)
	compileOnlyApi(libs.jspecify)

	shadowed(libs.jakarta.json.api)
	shadowed(libs.jakarta.json.implementation)
}

backwardCompatibilityChecks {
	enabled = false // not yet released
}

tasks {
	compileJava {
		options.compilerArgs.addAll(listOf(
			"--add-modules", "jakarta.json",
			"--add-reads", "${javaModuleName}=jakarta.json"
		))
	}
	javadoc {
		(options as StandardJavadocDocletOptions).apply {
			addStringOption("-add-modules", "jakarta.json")
			addStringOption("-add-reads", "${javaModuleName}=jakarta.json")
		}
	}
	val extractLicenses = register("extractLicenses", Sync::class) {
		val classPathElements = configurations.shadowedClasspath.flatMap { it.elements }
		from(zipTree(classPathElements.map { it.single { file -> file.asFile.name.contains("jakarta.json-api") } })) {
			include("META-INF/LICENSE.md")
			rename { "LICENSE-jakarta-json.md" }
		}
		from(zipTree(classPathElements.map { it.single { file -> file.asFile.name.contains("parsson") } })) {
			include("META-INF/LICENSE.md")
			rename { "LICENSE-parsson.md" }
		}
		into(layout.buildDirectory.dir("licenses"))
	}
	shadowJar {
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
		relocate("jakarta.json", "org.junit.platform.configuration.processor.shadow.jakarta.json")
		relocate("org.eclipse.parsson", "org.junit.platform.configuration.processor.shadow.org.eclipse.parsson")
		exclude("META-INF/LICENSE.md", "META-INF/NOTICE.md")
		from(extractLicenses)
		mergeServiceFiles()
	}
}
