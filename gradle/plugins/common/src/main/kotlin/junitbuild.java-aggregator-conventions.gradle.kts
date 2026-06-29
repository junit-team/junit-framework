import junitbuild.compatibility.BackwardCompatibilityChecksExtension

plugins {
	id("junitbuild.java-library-conventions")
}

the<BackwardCompatibilityChecksExtension>().apply {
	enabled = false // already checked by individual projects
}
