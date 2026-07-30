/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static com.tngtech.archunit.base.DescribedPredicate.and;
import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ANONYMOUS_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.TOP_LEVEL_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameContaining;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameStartingWith;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByClassesThat;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.domain.properties.HasSourceCodeLocation;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

import org.apiguardian.api.API;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestReporter;
import org.junit.platform.commons.support.scanning.ClasspathScanner;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;

@AnalyzeClasses(packages = { "org.junit.platform", "org.junit.jupiter", "org.junit.vintage" })
class ArchUnitTests {

	@SuppressWarnings("unused")
	@ArchTest
	private final ArchRule allClassesAreInJUnitPackage = classes() //
			.should().haveNameMatching("org\\.junit\\..+");

	@SuppressWarnings("unused")
	@ArchTest
	private final ArchRule allPublicTopLevelTypesHaveApiAnnotations = classes() //
			.that(have(modifier(PUBLIC))) //
			.and(TOP_LEVEL_CLASSES) //
			.and(not(ANONYMOUS_CLASSES)) //
			.and(not(describe("are Kotlin SAM type implementations", simpleName("")))) //
			.and(not(describe("are Kotlin-generated classes that contain only top-level functions",
				simpleNameEndingWith("Kt")))) //
			.and(notShadowed()) //
			.should().beAnnotatedWith(API.class);

	@SuppressWarnings("unused")
	@ArchTest // Consistency of @Documented and @Inherited is checked by the compiler but not for @Retention and @Target
	private final ArchRule repeatableAnnotationsShouldHaveMatchingContainerAnnotations = classes() //
			.that(nameStartingWith("org.junit.")) //
			.and().areAnnotations() //
			.and().areAnnotatedWith(Repeatable.class) //
			.should(haveContainerAnnotationWithSameRetentionPolicy()) //
			.andShould(haveContainerAnnotationWithSameTargetTypes());

	private final DescribedPredicate<? super JavaClass> jupiterAssertions = name(Assertions.class.getName()) //
			.or(name(Assumptions.class.getName())).or(name("org.junit.jupiter.api.AssertionsKt"));

	@SuppressWarnings("unused")
	@ArchTest // https://github.com/junit-team/junit-framework/issues/4604
	private final ArchRule jupiterAssertionsShouldBeSelfContained = classes().that(jupiterAssertions) //
			.should(onlyBeAccessedByClassesThat(jupiterAssertions));

	@SuppressWarnings("unused")
	@ArchTest
	private final ArchRule deprecatedAnnotationOnMembersShouldBeDeclaredConsistently = members() //
			.that(annotatedWith(Deprecated.class)) //
			.or(haveApiAnnotationWithDeprecatedStatus()) //
			.and(declaredIn(notShadowed())) //
			.should(haveBothAnnotationsWithMatchingSinceAttributes());

	@SuppressWarnings("unused")
	@ArchTest
	private final ArchRule deprecatedAnnotationOnClassesShouldBeDeclaredConsistently = classes() //
			.that(annotatedWith(Deprecated.class)) //
			.or(haveApiAnnotationWithDeprecatedStatus()) //
			.and(notShadowed()) //
			.should(haveBothAnnotationsWithMatchingSinceAttributes());

	@ArchTest
	void packagesShouldBeNullMarked(JavaClasses classes) {
		var exclusions = Stream.of( //
			"..shadow.." //
		).map(PackageMatcher::of).toList();

		var subpackages = Stream.of("org.junit.platform", "org.junit.jupiter", "org.junit.vintage") //
				.map(classes::getPackage) //
				.flatMap(rootPackage -> rootPackage.getSubpackagesInTree().stream()) //
				.filter(pkg -> exclusions.stream().noneMatch(it -> it.matches(pkg.getName()))) //
				.filter(pkg -> !pkg.getClasses().isEmpty()) //
				.toList();
		assertThat(subpackages).isNotEmpty();

		var violations = subpackages.stream() //
				.filter(pkg -> !pkg.isAnnotatedWith(NullMarked.class)) //
				.map(JavaPackage::getName) //
				.sorted();
		assertThat(violations).describedAs("The following packages are missing the @NullMarked annotation").isEmpty();
	}

	@ArchTest
	void allAreIn(JavaClasses classes) {
		// about 928 classes found in all jars
		assertTrue(classes.size() > 800, "expected more than 800 classes, got: " + classes.size());
	}

	@ArchTest
	void freeOfGroupCycles(JavaClasses classes) {
		slices().matching("org.junit.(*)..").should().beFreeOfCycles().check(classes);
	}

	@ArchTest
	@SuppressWarnings("removal")
	void freeOfPackageCycles(JavaClasses classes) throws Exception {
		slices().matching("org.junit.(**)").should().beFreeOfCycles() //

				// Ignore shadowed packages
				.ignoreDependency(nameContaining(".shadow."), nameContaining(".shadow.")) //

				// https://github.com/junit-team/junit-framework/issues/4886
				.ignoreDependency(TestReporter.class, org.junit.jupiter.api.extension.MediaType.class) //

				// https://github.com/junit-team/junit-framework/issues/4885
				.ignoreDependency(ClasspathScanner.class, org.junit.platform.commons.support.Resource.class) //

				// https://github.com/junit-team/junit-framework/issues/4919
				.ignoreDependency(org.junit.jupiter.params.support.ParameterInfo.class,
					org.junit.jupiter.params.ParameterInfo.class)

				// https://github.com/junit-team/junit-framework/issues/4923
				.ignoreDependency(org.junit.platform.engine.reporting.OutputDirectoryProvider.class,
					OutputDirectoryCreator.class) //
				.ignoreDependency(Class.forName("org.junit.platform.engine.reporting.OutputDirectoryProviderAdapter"),
					OutputDirectoryCreator.class) //
				.ignoreDependency(Class.forName("org.junit.platform.engine.reporting.OutputDirectoryProviderAdapter"),
					TestDescriptor.class) //

				.check(classes);
	}

	@ArchTest
	void avoidJavaUtilLogging(JavaClasses classes) {
		// LoggerFactory.java:80 -> sets field LoggerFactory$DelegatingLogger.julLogger
		var subset = classes.that(are(not(name("org.junit.platform.commons.logging.LoggerFactory$DelegatingLogger"))));
		GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(subset);
	}

	@ArchTest
	void avoidThrowingGenericExceptions(JavaClasses classes) {
		// LoggerFactory.java:155 -> new Throwable()
		var subset = classes.that(are(not(
			name("org.junit.platform.commons.logging.LoggerFactory$DelegatingLogger").or(nameContaining(".shadow.")))));
		GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(subset);
	}

	@ArchTest
	void avoidAccessingStandardStreams(JavaClasses classes) {
		// ConsoleLauncher, StreamInterceptor, Picocli et al...
		var subset = classes //
				.that(are(not(name("org.junit.platform.console.ConsoleLauncher")))) //
				.that(are(not(name("org.junit.platform.console.command.ConsoleTestExecutor")))) //
				.that(are(not(name("org.junit.platform.launcher.core.StreamInterceptor")))) //
				.that(are(not(name("org.junit.platform.testkit.engine.Events")))) //
				.that(are(not(name("org.junit.platform.testkit.engine.Executions")))) //
				//The PreInterruptThreadDumpPrinter writes to StdOut by contract to dump threads
				.that(are(not(name("org.junit.jupiter.engine.extension.PreInterruptThreadDumpPrinter")))) //
				.that(are(not(nameContaining(".shadow."))));
		GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(subset);
	}

	private static ArchCondition<? super JavaClass> haveContainerAnnotationWithSameRetentionPolicy() {
		return ArchCondition.from(new RepeatableAnnotationPredicate<>(Retention.class,
			(expectedTarget, actualTarget) -> expectedTarget.value() == actualTarget.value()));
	}

	private static ArchCondition<? super JavaClass> haveContainerAnnotationWithSameTargetTypes() {
		return ArchCondition.from(new RepeatableAnnotationPredicate<>(Target.class,
			(expectedTarget, actualTarget) -> Arrays.equals(expectedTarget.value(), actualTarget.value())));
	}

	private static <T extends HasAnnotations<?> & HasSourceCodeLocation> ArchCondition<T> haveBothAnnotationsWithMatchingSinceAttributes() {
		return ArchCondition.from( //
			DescribedPredicate.and( //
				annotatedWith(Deprecated.class), haveApiAnnotationWithDeprecatedStatus(), //
				haveSinceAttributeMatchingApiAnnotation()));
	}

	private static <T extends HasAnnotations<?> & HasSourceCodeLocation> DescribedPredicate<T> haveApiAnnotationWithDeprecatedStatus() {
		return and(annotatedWith(API.class), describe("status() is DEPRECATED",
			element -> element.getAnnotationOfType(API.class).status() == DEPRECATED));
	}

	private static <T extends HasAnnotations<?> & HasSourceCodeLocation> DescribedPredicate<T> haveSinceAttributeMatchingApiAnnotation() {
		return describe("@API(since) equals @Deprecated(since)", element -> {
			var deprecatedAnnotation = element.getAnnotationOfType(Deprecated.class);
			var apiAnnotation = element.getAnnotationOfType(API.class);
			return deprecatedAnnotation.since() != null //
					&& Objects.equals(deprecatedAnnotation.since(), apiAnnotation.since());
		});
	}

	private static DescribedPredicate<JavaClass> notShadowed() {
		return not(describe("are shadowed", resideInAPackage("..shadow..")));
	}

	private static class RepeatableAnnotationPredicate<T extends Annotation> extends DescribedPredicate<JavaClass> {

		private final Class<T> annotationType;
		private final BiPredicate<T, T> predicate;

		RepeatableAnnotationPredicate(Class<T> annotationType, BiPredicate<T, T> predicate) {
			super("have identical @%s annotation as container annotation", annotationType.getSimpleName());
			this.annotationType = annotationType;
			this.predicate = predicate;
		}

		@Override
		public boolean test(JavaClass annotationClass) {
			var containerAnnotationClass = (JavaClass) annotationClass.getAnnotationOfType(
				Repeatable.class.getName()).get("value").orElseThrow();
			var expectedAnnotation = annotationClass.tryGetAnnotationOfType(annotationType);
			var actualAnnotation = containerAnnotationClass.tryGetAnnotationOfType(annotationType);
			return expectedAnnotation.map(expectedTarget -> actualAnnotation //
					.map(actualTarget -> predicate.test(expectedTarget, actualTarget)) //
					.orElse(false)) //
					.orElse(actualAnnotation.isEmpty());
		}
	}

}
