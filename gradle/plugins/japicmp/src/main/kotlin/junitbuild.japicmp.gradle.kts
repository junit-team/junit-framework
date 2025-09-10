
import de.undercouch.gradle.tasks.download.Download
import junitbuild.japicmp.InternalApiFilter
import junitbuild.japicmp.JApiCmpExtension
import junitbuild.japicmp.UnacceptedIncompatibilityRule
import junitbuild.japicmp.UnacceptedSuperClassChangeRule
import me.champeau.gradle.japicmp.JapicmpTask
import me.champeau.gradle.japicmp.report.stdrules.RecordSeenMembersSetup

plugins {
	java
	id("de.undercouch.download")
	id("me.champeau.gradle.japicmp")
}

val extension = extensions.create<JApiCmpExtension>("japicmp").apply {
	acceptedIncompatibilities.apply {
		val acceptedBreakingChangesFile = rootProject.layout.projectDirectory.file("gradle/config/japicmp/accepted-breaking-changes.txt")
		if (acceptedBreakingChangesFile.asFile.exists()) {
			convention(providers.fileContents(acceptedBreakingChangesFile).asText.map { it.lines() })
		} else {
			empty()
		}
		finalizeValueOnRead()
	}
	previousVersion.apply {
		convention(provider {
			if (group == "org.junit.platform") "1.13.4" else "5.13.4"
		})
		finalizeValueOnRead()
	}
}

val downloadPreviousReleaseJar by tasks.registering(Download::class) {
	val previousVersion = extension.previousVersion.get()
	src("https://repo1.maven.org/maven2/${project.group.toString().replace(".", "/")}/${project.name}/$previousVersion/${project.name}-$previousVersion.jar")
	dest(layout.buildDirectory.dir("japicmp"))
	overwrite(false)
	quiet(true)
}

val checkBackwardCompatibility by tasks.registering(JapicmpTask::class) {
	oldClasspath.from(downloadPreviousReleaseJar.map { it.outputFiles })
	newClasspath.from(tasks.jar)
	onlyModified = true
	ignoreMissingClasses = true
	txtOutputFile = layout.buildDirectory.file("reports/japicmp/plain-report.txt")
	mdOutputFile = layout.buildDirectory.file("reports/japicmp/plain-report.md")
	htmlOutputFile = layout.buildDirectory.file("reports/japicmp/plain-report.html")
	addExcludeFilter(InternalApiFilter::class.java)
	packageExcludes.add("*.shadow.*")
	inputs.property("acceptedIncompatibilities", extension.acceptedIncompatibilities)
	richReport {
		destinationDir = layout.buildDirectory.dir("reports/japicmp")
		addSetupRule(RecordSeenMembersSetup::class.java)
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
			addRule(UnacceptedIncompatibilityRule::class.java, params)
			addRule(UnacceptedSuperClassChangeRule::class.java, params)
		}
	}
}
