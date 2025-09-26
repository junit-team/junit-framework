import de.undercouch.gradle.tasks.download.Download
import junitbuild.extensions.javaModuleName
import junitbuild.japicmp.AcceptedViolationSuppressor
import junitbuild.japicmp.AcceptedViolationsPostProcessRule
import junitbuild.japicmp.BreakingSuperClassChangeRule
import junitbuild.japicmp.InternalApiFilter
import junitbuild.japicmp.JApiCmpExtension
import junitbuild.japicmp.SourceIncompatibleRule
import me.champeau.gradle.japicmp.JapicmpTask
import me.champeau.gradle.japicmp.report.stdrules.BinaryIncompatibleRule
import me.champeau.gradle.japicmp.report.stdrules.RecordSeenMembersSetup

plugins {
	java
	id("de.undercouch.download")
	id("me.champeau.gradle.japicmp")
}

val extension = extensions.create<JApiCmpExtension>("japicmp").apply {
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
		convention(provider {
			if (group == "org.junit.platform") "1.14.0-RC1" else "5.14.0-RC1"
		})
		finalizeValueOnRead()
	}
}

val downloadPreviousReleaseJar by tasks.registering(Download::class) {
	onlyIf { extension.enabled.get() }
	val previousVersion = extension.previousVersion.get()
	src("https://repo1.maven.org/maven2/${project.group.toString().replace(".", "/")}/${project.name}/$previousVersion/${project.name}-$previousVersion.jar")
	dest(layout.buildDirectory.dir("japicmp"))
	overwrite(false)
	quiet(true)
	retries(2)
	outputs.cacheIf { true }
}

val checkBackwardCompatibility by tasks.registering(JapicmpTask::class) {
	onlyIf { extension.enabled.get() }
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

tasks.check {
	dependsOn(checkBackwardCompatibility)
}

afterEvaluate {
	val params = mapOf(
		"acceptedIncompatibilities" to extension.acceptedIncompatibilities.get().joinToString(",")
	)
	checkBackwardCompatibility {
		richReport {
			addViolationTransformer(AcceptedViolationSuppressor::class.java, params)
			addPostProcessRule(AcceptedViolationsPostProcessRule::class.java, params)
		}
	}
}
