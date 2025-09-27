import junitbuild.japicmp.JApiCmpExtension

plugins {
	id("junitbuild.java-library-conventions")
}

tasks.javadoc {
	// Since this JAR contains no classes, running Javadoc fails with:
	// "No public or protected classes found to document"
	enabled = false
}

the<JApiCmpExtension>().apply {
	enabled = false // already checked by individual projects
}
