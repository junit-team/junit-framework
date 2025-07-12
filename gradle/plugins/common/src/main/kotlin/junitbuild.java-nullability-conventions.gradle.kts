import junitbuild.extensions.dependencyFromLibs
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
	`java-library`
	id("net.ltgt.errorprone")
	id("net.ltgt.nullaway")
}

dependencies {
	errorprone(dependencyFromLibs("errorProne-core"))
	errorprone(dependencyFromLibs("nullaway"))
}

nullaway {
	onlyNullMarked = true
}

tasks.withType<JavaCompile>().configureEach {
	options.errorprone {
		val onJ9 = java.toolchain.implementation.orNull == JvmImplementation.J9
		if (name == "compileJava" && !onJ9) {
			disable(

				// This check is opinionated wrt. which method names it considers unsuitable for import which includes
				// a few of our own methods in `ReflectionUtils` etc.
				"BadImport",

				// The findings of this check are subjective because a named constant can be more readable in many cases
				"UnnecessaryLambda",

				// Resolving findings for these checks requires ErrorProne's annotations which we don't want to use
				"AnnotateFormatMethod",
				"DoNotCallSuggester",
				"InlineMeSuggester",
				"ImmutableEnumChecker",

				// Resolving findings for this check requires using Guava which we don't want to use
				"StringSplitter",

				// Produces a lot of findings that we consider to be false positives, for example for package-private
				// classes and methods
				"MissingSummary",
			)
			error("PackageLocation")
		} else {
			disableAllChecks = true
		}
		nullaway {
			if (onJ9) {
				disable()
			} else {
				enable()
			}
			isJSpecifyMode = true
			customContractAnnotations.add("org.junit.platform.commons.annotation.Contract")
			checkContracts = true
		}
	}
}

tasks.withType<JavaCompile>().named { it.startsWith("compileTest") }.configureEach {
	options.errorprone.nullaway {
		handleTestAssertionLibraries = true
		excludedFieldAnnotations.addAll(
			"org.junit.jupiter.api.io.TempDir",
			"org.junit.jupiter.params.Parameter",
			"org.junit.runners.Parameterized.Parameter",
			"org.mockito.Captor",
			"org.mockito.InjectMocks",
			"org.mockito.Mock",
			"org.mockito.Spy",
		)
	}
}
