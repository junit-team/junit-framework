plugins {
	`java-library`
}

java {
	withJavadocJar()
}

tasks.javadoc {
	options {
		memberLevel = JavadocMemberLevel.PROTECTED
		header = project.name
		encoding = "UTF-8"
		locale = "en"
		(this as StandardJavadocDocletOptions).apply {
			addBooleanOption("Xdoclint:all,-missing,-reference", true)
			addBooleanOption("XD-Xlint:none", true)
			addBooleanOption("html5", true)
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
