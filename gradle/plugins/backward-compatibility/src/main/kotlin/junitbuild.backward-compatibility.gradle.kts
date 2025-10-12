
import de.undercouch.gradle.tasks.download.Download
import junitbuild.compatibility.BackwardCompatibilityChecksExtension
import junitbuild.compatibility.japicmp.AcceptedViolationSuppressor
import junitbuild.compatibility.japicmp.AcceptedViolationsPostProcessRule
import junitbuild.compatibility.japicmp.BreakingSuperClassChangeRule
import junitbuild.compatibility.japicmp.InternalApiFilter
import junitbuild.compatibility.japicmp.SourceIncompatibleRule
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

val roseau by tasks.registering(JavaExec::class) {
	if (gradle.startParameter.isOffline) {
		enabled = false
	}
	onlyIf { extension.enabled.get() }
	onlyIf("https://github.com/alien-tools/roseau/issues/90") { !OperatingSystem.current().isWindows }

	mainClass = "io.github.alien.roseau.cli.RoseauCLI"
	classpath = files(roseauClasspath)

	inputs.files(configurations.compileClasspath)
		.withNormalizer(CompileClasspathNormalizer::class)
		.withPropertyName("apiClasspath")

	val v1Jar = downloadPreviousReleaseJar.map { it.outputFiles.single() }
	inputs.file(v1Jar)
		.withNormalizer(CompileClasspathNormalizer::class)
		.withPropertyName("v1")

	val v2Jar = tasks.jar.flatMap { it.archiveFile }.map { it.asFile }
	inputs.file(v2Jar)
		.withNormalizer(CompileClasspathNormalizer::class)
		.withPropertyName("v2")

	outputs.file(roseauCsvFile)
		.withPropertyName("report")

	argumentProviders.add(CommandLineArgumentProvider {
		listOf(
			"--classpath", configurations.compileClasspath.get().asPath,
			"--v1", v1Jar.get().absolutePath,
			"--v2", v2Jar.get().absolutePath,
			"--diff",
			"--report", roseauCsvFile.get().asFile.absolutePath,
		)
	})

	doFirst {
		roseauCsvFile.get().asFile.parentFile.mkdirs()
	}
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
