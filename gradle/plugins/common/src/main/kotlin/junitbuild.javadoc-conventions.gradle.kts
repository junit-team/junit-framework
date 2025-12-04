import junitbuild.javadoc.JavadocConventionsExtension

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
