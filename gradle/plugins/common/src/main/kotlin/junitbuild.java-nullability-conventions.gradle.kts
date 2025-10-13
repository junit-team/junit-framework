import junitbuild.extensions.dependencyFromLibs
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
	`java-library`
	id("net.ltgt.errorprone")
	id("net.ltgt.nullaway")
}

dependencies {
	errorprone(dependencyFromLibs("error-prone-contrib"))
	errorprone(dependencyFromLibs("error-prone-core"))
	errorprone(dependencyFromLibs("nullaway"))
	constraints {
		errorprone("com.google.guava:guava") {
			version {
				require("33.4.8-jre")
			}
			because("Older versions use deprecated methods in sun.misc.Unsafe")
			// https://github.com/junit-team/junit-framework/pull/5039#discussion_r2414490581
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.errorprone {
		val shouldDisableErrorProne = java.toolchain.implementation.orNull == JvmImplementation.J9
		if (name == "compileJava" && !shouldDisableErrorProne) {
			disable(
				"AnnotateFormatMethod", // We don`t want to use ErrorProne's annotations.
				"BadImport", // This check is opinionated wrt. which method names it considers unsuitable for import which includes a few of our own methods in `ReflectionUtils` etc.
				"DoNotCallSuggester", // We don`t want to use ErrorProne's annotations.
				"ImmutableEnumChecker", // We don`t want to use ErrorProne's annotations.
				"InlineMeSuggester", // We don`t want to use ErrorProne's annotations.
				"MissingSummary", // Produces a lot of findings that we consider to be false positives, for example for package-private classes and methods.
				"StringSplitter", // We don`t want to use Guava.
				"UnnecessaryLambda", // The findings of this check are subjective because a named constant can be more readable in many cases.
				// picnic (https://error-prone.picnic.tech)
				"ConstantNaming",
				"DirectReturn", // We don`t want to use this: https://github.com/junit-team/junit-framework/pull/5006#discussion_r2403984446
				"FormatStringConcatenation",
				"IdentityConversion",
				"LexicographicalAnnotationAttributeListing", // We don`t want to use this: https://github.com/junit-team/junit-framework/pull/5043#pullrequestreview-3330615838
				"LexicographicalAnnotationListing",
				"MissingTestCall",
				"NestedOptionals",
				"NonStaticImport",
				"OptionalOrElseGet",
				"PrimitiveComparison",
				"StaticImport",
				"TimeZoneUsage",
			)
			error(
				"PackageLocation",
				"RedundantStringConversion",
				"RedundantStringEscape",
			)
		} else {
			disableAllChecks = true
		}
		nullaway {
			if (shouldDisableErrorProne) {
				disable()
			} else {
				enable()
			}
			onlyNullMarked = true
			isJSpecifyMode = true
			customContractAnnotations.add("org.junit.platform.commons.annotation.Contract")
			checkContracts = true
			suppressionNameAliases.add("DataFlowIssue")
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
