import junitbuild.javadoc.JavadocConventionsExtension
import java.nio.file.Files
import kotlin.io.path.writeLines

plugins {
	`java-library`
}

java {
	withJavadocJar()
}

val javadocReference = configurations.dependencyScope("javadocReference")

val extension = JavadocConventionsExtension(project, javadocReference.get(), tasks.javadoc)
project.extensions.add("javadocConventions", extension)

val javadocClasspath = configurations.resolvable("javadocClasspath") {
	extendsFrom(configurations.compileClasspath.get())
	extendsFrom(javadocReference.get())
}

tasks.javadoc {
	classpath = javadocClasspath.get()
	options {
		memberLevel = JavadocMemberLevel.PROTECTED
		header = project.name
		encoding = "UTF-8"
		locale = "en"
		(this as StandardJavadocDocletOptions).apply {
			addBooleanOption("Xdoclint:all,-missing", true)
			addBooleanOption("html5", true)
			addBooleanOption("Werror", true)
			addMultilineStringsOption("tag").value = listOf(
				"apiNote:a:API Note:",
				"implNote:a:Implementation Note:"
			)
			use(true)
			noTimestamp(true)
		}
	}
}

tasks.named<Jar>("javadocJar").configure {
	from(tasks.javadoc.map { File(it.destinationDir, "element-list") }) {
		// For compatibility with older tools, e.g. NetBeans 11
		rename { "package-list" }
	}
}

val extractJavadocSinceValues by tasks.registering {
	inputs.files(sourceSets.main.get().allJava).withPathSensitivity(PathSensitivity.NONE)
	val outputFile = layout.buildDirectory.file("docs/javadoc-since-values.txt")
	outputs.file(outputFile)
	outputs.cacheIf { true }
	doFirst {
		val regex = "\\s+(?:\\*|///) @since ([0-9.]+).*".toRegex()
		val values = sourceSets.main.get().allJava.files.asSequence()
			.flatMap { file -> file.readLines().asSequence() }
			.mapNotNull(regex::matchEntire)
			.map { result -> result.groupValues[1] }
			.sorted()
			.distinct()
		with(outputFile.get().asFile.toPath()) {
			Files.createDirectories(parent)
			writeLines(values)
		}
	}
}

configurations.consumable("javadocSinceValues") {
	attributes {
		attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("javadoc-since-values"))
	}
	outgoing {
		artifact(extractJavadocSinceValues)
	}
}
