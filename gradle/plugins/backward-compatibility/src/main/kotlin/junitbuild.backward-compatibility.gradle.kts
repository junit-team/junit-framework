
import de.undercouch.gradle.tasks.download.Download
import junitbuild.compatibility.BackwardCompatibilityChecksExtension
import junitbuild.compatibility.japicmp.AcceptedViolationSuppressor
import junitbuild.compatibility.japicmp.AcceptedViolationsPostProcessRule
import junitbuild.compatibility.japicmp.BreakingSuperClassChangeRule
import junitbuild.compatibility.japicmp.InternalApiFilter
import junitbuild.compatibility.japicmp.SourceIncompatibleRule
import junitbuild.compatibility.roseau.RoseauDiff
import junitbuild.extensions.dependencyFromLibs
import junitbuild.extensions.javaModuleName
import me.champeau.gradle.japicmp.JapicmpTask
import me.champeau.gradle.japicmp.report.stdrules.BinaryIncompatibleRule
import me.champeau.gradle.japicmp.report.stdrules.RecordSeenMembersSetup
import org.gradle.internal.os.OperatingSystem

plugins {
	java
	id("de.undercouch.download")
	id("me.champeau.gradle.japicmp")
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
	acceptedIncompatibilities.apply {
		val acceptedBreakingChangesFile = rootProject.layout.projectDirectory.file("gradle/config/japicmp/accepted-breaking-changes.txt")
		if (acceptedBreakingChangesFile.asFile.exists()) {
			convention(providers.fileContents(acceptedBreakingChangesFile).asText
				.map { it.lineSequence().filter { line -> line.startsWith(project.javaModuleName) }.toList() })
		} else {
			empty()
		}
		finalizeValueOnRead()
	}
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
	dest(layout.buildDirectory.dir("japicmp"))
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

val japicmp by tasks.registering(JapicmpTask::class) {
	if (gradle.startParameter.isOffline) {
		enabled = false
	}
	onlyIf { extension.enabled.get() }
	shouldRunAfter(roseau)

	oldClasspath.from(downloadPreviousReleaseJar.map { it.outputFiles })
	newClasspath.from(tasks.jar)
	onlyModified = true
	ignoreMissingClasses = true
	htmlOutputFile = layout.buildDirectory.file("reports/japicmp/plain-report.html")
	addExcludeFilter(InternalApiFilter::class.java)
	packageExcludes.add("*.shadow.*")
	inputs.property("acceptedIncompatibilities", extension.acceptedIncompatibilities)
	richReport {
		title = "Compatibility report"
		description = extension.previousVersion.map { "and source compatibility compared against $it" }
		destinationDir = layout.buildDirectory.dir("reports/japicmp")
		addSetupRule(RecordSeenMembersSetup::class.java)
		addRule(BreakingSuperClassChangeRule::class.java)
		addRule(BinaryIncompatibleRule::class.java)
		addRule(SourceIncompatibleRule::class.java)
	}
}

val checkBackwardCompatibility by tasks.registering {
	dependsOn(roseau, japicmp)
}

tasks.check {
	dependsOn(checkBackwardCompatibility)
}

afterEvaluate {
	val params = mapOf(
		"acceptedIncompatibilities" to extension.acceptedIncompatibilities.get().joinToString(",")
	)
	japicmp {
		richReport {
			addViolationTransformer(AcceptedViolationSuppressor::class.java, params)
			addPostProcessRule(AcceptedViolationsPostProcessRule::class.java, params)
		}
	}
}
