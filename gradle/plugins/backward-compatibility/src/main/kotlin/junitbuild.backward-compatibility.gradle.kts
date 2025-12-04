
import de.undercouch.gradle.tasks.download.Download
import junitbuild.compatibility.BackwardCompatibilityChecksExtension
import junitbuild.compatibility.roseau.RoseauDiff
import junitbuild.extensions.dependencyFromLibs

plugins {
	java
	id("de.undercouch.download")
}

val roseauDependencies = configurations.dependencyScope("roseau")
val roseauClasspath = configurations.resolvable("roseauClasspath") {
	extendsFrom(roseauDependencies.get())
}
dependencies {
	roseauDependencies(dependencyFromLibs("roseau-cli"))
	constraints {
		roseauDependencies("org.apache.commons:commons-lang3") {
			version {
				require("3.18.0")
			}
			because("Workaround for CVE-2025-48924")
		}
	}
}

val extension = extensions.create<BackwardCompatibilityChecksExtension>("backwardCompatibilityChecks").apply {
	enabled.convention(true)
	previousVersion.apply {
		convention(providers.gradleProperty("apiBaselineVersion"))
		finalizeValueOnRead()
	}
}

val downloadPreviousReleaseJar by tasks.registering(Download::class) {
	if (gradle.startParameter.isOffline) {
		enabled = false
	}
	onlyIf { extension.enabled.get() }
	val previousVersion = extension.previousVersion.get()
	src("https://repo1.maven.org/maven2/${project.group.toString().replace(".", "/")}/${project.name}/$previousVersion/${project.name}-$previousVersion.jar")
	dest(layout.buildDirectory.dir("previousRelease"))
	overwrite(false)
	quiet(true)
	retries(2)
	outputs.cacheIf { true }
}

val roseauCsvFile = layout.buildDirectory.file("reports/roseau/breaking-changes.csv")

val roseau by tasks.registering(RoseauDiff::class) {
	if (gradle.startParameter.isOffline) {
		enabled = false
	}
	onlyIf { extension.enabled.get() }

	toolClasspath.from(roseauClasspath)
	libraryClasspath.from(configurations.compileClasspath)
	v1 = downloadPreviousReleaseJar.map { it.outputFiles.single() }
	v2 = tasks.jar.flatMap { it.archiveFile }.map { it.asFile }
	configFile = rootProject.layout.projectDirectory.file("gradle/config/roseau/config.yaml")
	rootProject.layout.projectDirectory.file("gradle/config/roseau/accepted-breaking-changes.csv").asFile.let {
		if (it.exists()) {
			acceptedChangesCsvFile = it
		}
	}
	reportDir = layout.buildDirectory.dir("reports/roseau")
}

val checkBackwardCompatibility by tasks.registering {
	dependsOn(roseau)
}

tasks.check {
	dependsOn(checkBackwardCompatibility)
}
