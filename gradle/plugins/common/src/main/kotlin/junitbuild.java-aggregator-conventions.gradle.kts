import junitbuild.compatibility.BackwardCompatibilityChecksExtension

plugins {
	id("junitbuild.java-library-conventions")
}

tasks.javadoc {
	// Since this JAR contains no classes, running Javadoc fails with:
	// "No public or protected classes found to document"
	enabled = false
}

the<BackwardCompatibilityChecksExtension>().apply {
	enabled = false // already checked by individual projects
}
