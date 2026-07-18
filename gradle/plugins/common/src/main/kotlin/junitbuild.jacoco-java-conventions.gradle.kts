import org.gradle.api.attributes.LibraryElements.CLASSES
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE

plugins {
	java
	id("junitbuild.build-parameters")
	id("junitbuild.jacoco-conventions")
}

@Suppress("UNCHECKED_CAST")
val mavenizedProjects = rootProject.extra["mavenizedProjects"] as List<Project>

tasks.withType<Test>().configureEach {
	configure<JacocoTaskExtension> {
		isEnabled = buildParameters.testing.enableJaCoCo
	}
}

val codeCoverageClassesJar = tasks.register("codeCoverageClassesJar", Jar::class) {
	from(tasks.jar.map { zipTree(it.archiveFile) })
	archiveClassifier = "jacoco"
	enabled = project in mavenizedProjects
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

configurations.consumable("codeCoverageReportClasses") {
	attributes {
		attribute(LIBRARY_ELEMENTS_ATTRIBUTE, named(CLASSES))
	}
	outgoing.artifact(codeCoverageClassesJar)
}
