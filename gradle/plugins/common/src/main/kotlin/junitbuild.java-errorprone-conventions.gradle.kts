import junitbuild.extensions.dependencyFromLibs
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.jvm.toolchain.JvmImplementation.J9
import java.lang.System.getenv

plugins {
	`java-library`
	id("net.ltgt.errorprone")
	id("net.ltgt.nullaway")
}

dependencies {
	errorprone(dependencyFromLibs("error-prone-contrib"))
	errorprone(dependencyFromLibs("error-prone-core"))
	errorprone(dependencyFromLibs("nullaway"))
}

tasks.withType<JavaCompile>().configureEach {
	options.errorprone {
		val enableErrorProne = java.toolchain.implementation.orNull != J9
		if (enableErrorProne && name == "compileJava") {
			disableAllWarnings = true // considering this immense spam burden, remove this once to fix dedicated flaw. https://github.com/diffplug/spotless/pull/2766
			disable( // We don`t want to use ErrorProne's annotations.
				// picnic (https://error-prone.picnic.tech)
				"ConstantNaming",
				"DirectReturn", // We don`t want to use this. https://github.com/junit-team/junit-framework/pull/5006#discussion_r2403984446
				"FormatStringConcatenation",
				"IdentityConversion",
				"LexicographicalAnnotationAttributeListing", // We don`t want to use this. https://github.com/junit-team/junit-framework/pull/5043#pullrequestreview-3330615838
				"LexicographicalAnnotationListing",
				"MissingTestCall",
				"NestedOptionals",
				"OptionalOrElseGet",
				"PrimitiveComparison",
				"StaticImport",
				"TimeZoneUsage",
			)
			error(
				"CanonicalAnnotationSyntax",
				"IsInstanceLambdaUsage",
				"MissingOverride",
				"NonStaticImport",
				"PackageLocation",
				"RedundantStringConversion",
				"RedundantStringEscape",
				"SelfAssignment",
				"StringCharset",
				"StringJoin",
			)
			if (!getenv().containsKey("CI") && getenv("IN_PLACE").toBoolean()) {
				errorproneArgs.addAll(
					"-XepPatchLocation:IN_PLACE",
					"-XepPatchChecks:" +
							"NonStaticImport,"
				)
			}
		} else {
			disableAllChecks = true
		}
		nullaway {
			if (enableErrorProne) {
				enable()
			} else {
				disable()
			}
			checkContracts = true
			isJSpecifyMode = true
			onlyNullMarked = true
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
