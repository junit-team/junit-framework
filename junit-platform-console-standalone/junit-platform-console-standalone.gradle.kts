import junitbuild.extensions.withArchiveOperations
import junitbuild.java.WriteArtifactsFile

plugins {
	id("junitbuild.java-library-conventions")
	id("junitbuild.shadow-conventions")
}

description = "JUnit Platform Console Standalone"

dependencies {
	shadowed(projects.junitPlatformReporting)
	shadowed(projects.junitPlatformConsole)
	shadowed(projects.junitPlatformSuiteEngine)
	shadowed(projects.junitJupiterEngine)
	shadowed(projects.junitJupiterParams)
	shadowed(projects.junitVintageEngine)
	shadowed(libs.apiguardian) {
		because("downstream projects need it to avoid compiler warnings")
	}

	osgiVerification(libs.openTestReporting.tooling.spi)
}

backwardCompatibilityChecks {
	enabled = false // already checked by individual projects
}

tasks {
	jar {
		manifest {
			attributes("Automatic-Module-Name" to "org.junit.platform.console.standalone")
			attributes("Main-Class" to "org.junit.platform.console.ConsoleLauncher")
		}
	}
	val shadowedArtifactsFile by registering(WriteArtifactsFile::class) {
		from(configurations.shadowedClasspath)
		outputFile = layout.buildDirectory.file("shadowed-artifacts")
	}
	val extractThirdPartyLicenses by registering(Sync::class) {
		from(withArchiveOperations { ops -> configurations.shadowedClasspath.flatMap { it.elements }.map { it.map(ops::zipTree) } })
		into(layout.buildDirectory.dir("thirdPartyLicenses"))
		include("LICENSE.txt")
		include("LICENSE-junit.txt")
		include("META-INF/LICENSE-*")
		exclude("META-INF/LICENSE-notice.md")
		eachFile {
			val fileName = relativePath.lastName
			relativePath = RelativePath(true, when (fileName) {
				"LICENSE.txt" -> "LICENSE-hamcrest"
				"LICENSE-junit.txt" -> "LICENSE-junit4"
				else -> fileName
			})
		}
		includeEmptyDirs = false
	}
	shadowJar {
		// https://github.com/junit-team/junit-framework/issues/2557
		// exclude compiled module declarations from any source (e.g. /*, /META-INF/versions/N/*)
		exclude("**/module-info.class")
		// https://github.com/junit-team/junit-framework/issues/761
		// prevent duplicates, add 3rd-party licenses explicitly
		exclude("**/COPYRIGHT*")
		exclude("META-INF/LICENSE*")
		exclude("LICENSE*.txt") // JUnit 4 and Hamcrest
		from(extractThirdPartyLicenses) {
			into("META-INF")
		}
		from(shadowedArtifactsFile) {
			into("META-INF")
		}

		bundle {
			val importAPIGuardian: String by extra
			val importJSpecify: String by extra
			bnd("""
				# Customize the imports because this is an aggregate jar
				Import-Package: \
					$importAPIGuardian,\
					$importJSpecify,\
					kotlin.*;resolution:="optional",\
					kotlinx.*;resolution:="optional",\
					*
				# Disable the APIGuardian plugin since everything was already
				# processed, again because this is an aggregate jar
				-export-apiguardian:
			""")
		}

		duplicatesStrategy = DuplicatesStrategy.INCLUDE
		mergeServiceFiles()
		failOnDuplicateEntries = true

		manifest.apply {
			attributes(mapOf(
					"Specification-Title" to project.name,
					"Implementation-Title" to project.name,
					// Generate test engine version information in single shared manifest file.
					// Pattern of key and value: `"Engine-Version-{YourTestEngine#getId()}": "47.11"`
					"Engine-Version-junit-jupiter" to project.version,
					"Engine-Version-junit-vintage" to project.version,
			))
		}
	}
}
