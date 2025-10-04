import junitbuild.extensions.dependencyFromLibs
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
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
	errorprone(dependencyFromLibs("refaster-runner"))
	constraints {
		errorprone("com.google.guava:guava") {
			version {
				require("33.4.8-jre")
			}
			because("Older versions use deprecated methods in sun.misc.Unsafe")
		}
	}
}

nullaway {
	onlyNullMarked = true
}

tasks.withType<JavaCompile>().configureEach {
	options.errorprone {
		allErrorsAsWarnings = true
		disableWarningsInGeneratedCode = true
		errorproneArgs.add("-XepOpt:Refaster:NamePattern=^(?!.*Rules\\$).*") // might consider.
		if (getenv("IN_PLACE").toBoolean()) {
			errorproneArgs.addAll(
				"-XepPatchLocation:IN_PLACE", // why only certain picnic rules apply?
				"-XepPatchChecks:" +
					"AddNullMarkedToPackageInfo," +
					"AmbiguousJsonCreator," +
					"AssertJNullnessAssertion," +
					"AutowiredConstructor," +
					"CanonicalAnnotationSyntax," +
					"CanonicalClassNameUsage," +
					"ClassCastLambdaUsage," +
					"CollectorMutability," +
					"ConstantNaming," +
					"DeadException," +
					"DefaultCharset," +
					"EagerStringFormatting," +
					"EmptyMethod," +
					"EmptyMonoZip," +
					"ExplicitArgumentEnumeration," +
					"ExplicitEnumOrdering," +
					"FluxFlatMapUsage," +
					"FluxImplicitBlock," +
					"FormatStringConcatenation," +
					"IdentityConversion," +
					"ImmutablesSortedSetComparator," +
					"IsInstanceLambdaUsage," +
					"JUnitClassModifiers," +
					"JUnitMethodDeclaration," +
					"JUnitNullaryParameterizedTestDeclaration," +
					"JUnitValueSource," +
					"LexicographicalAnnotationAttributeListing," +
					"LexicographicalAnnotationListing," +
					"MissingOverride," +
					"MockitoMockClassReference," +
					"MockitoStubbing," +
					"MongoDBTextFilterUsage," +
					"NestedOptionals," +
					"NestedPublishers," +
					"NonEmptyMono," +
					"NonStaticImport," +
					"OptionalOrElseGet," +
					"PrimitiveComparison," +
					"RedundantStringConversion," +
					"RedundantStringEscape," +
					"RefasterAnyOfUsage," +
					"ImmutableEnumChecker," +
					"RequestMappingAnnotation," +
					"RequestParamType," +
					"Slf4jLogStatement," +
					"Slf4jLoggerDeclaration," +
					"SpringMvcAnnotation," +
					"StaticImport," +
					"StringJoin," +
					"TimeZoneUsage"
			)
		}
		val shouldDisableErrorProne = java.toolchain.implementation.orNull == JvmImplementation.J9
		if (name == "compileJava" && !shouldDisableErrorProne) {
			disable(
				"AnnotateFormatMethod", // We don't want to use ErrorProne's annotations.
				"BadImport", // This check is opinionated wrt. which method names it considers unsuitable for import which includes a few of our own methods in `ReflectionUtils` etc.
				"DirectReturn", // https://github.com/junit-team/junit-framework/pull/5006#discussion_r2403984446
				"DoNotCallSuggester",
				"ImmutableEnumChecker",
				"InlineMeSuggester",
				"MissingSummary", // Produces a lot of findings that we consider to be false positives, for example for package-private classes and methods.
				"StringSplitter", // We don't want to use Guava.
				"UnnecessaryLambda", // The findings of this check are subjective because a named constant can be more readable in many cases.
				// might consider: https://error-prone.picnic.tech
				"AddNullMarkedToPackageInfo",
				"AmbiguousJsonCreator",
				"AssertJNullnessAssertion",
				"AutowiredConstructor",
				"CanonicalAnnotationSyntax",
				"CanonicalClassNameUsage",
				"ClassCastLambdaUsage",
				"CollectorMutability",
				"ConstantNaming",
				"DeadException",
				"EagerStringFormatting",
				"EmptyMonoZip",
				"ExplicitArgumentEnumeration",
				"ExplicitEnumOrdering",
				"FluxFlatMapUsage",
				"FluxImplicitBlock",
				"FormatStringConcatenation",
				"IdentityConversion",
				"ImmutablesSortedSetComparator",
				"IsInstanceLambdaUsage",
				"JUnitClassModifiers",
				"JUnitMethodDeclaration",
				"JUnitNullaryParameterizedTestDeclaration",
				"JUnitValueSource",
				"LexicographicalAnnotationAttributeListing",
				"LexicographicalAnnotationListing",
				"MockitoMockClassReference",
				"MockitoStubbing",
				"NestedOptionals",
				"NestedPublishers",
				"NonEmptyMono",
				"NonStaticImport",
				"OptionalOrElseGet",
				"PrimitiveComparison",
				"RequestMappingAnnotation",
				"Slf4jLogStatement",
				"Slf4jLoggerDeclaration",
				"StaticImport",
				"TimeZoneUsage",
			)
			error("PackageLocation")
		} else {
			disableAllChecks = true
		}
		nullaway {
			if (shouldDisableErrorProne) {
				disable()
			} else {
				enable()
			}
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
