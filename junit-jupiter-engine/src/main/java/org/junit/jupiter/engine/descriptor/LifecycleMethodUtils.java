/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.jupiter.engine.support.MethodReflectionUtils.getReturnType;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedMethods;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.Condition.alwaysSatisfied;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ClassTemplateInvocationLifecycleMethod;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.Condition;

/**
 * Collection of utilities for working with test lifecycle methods.
 *
 * @since 5.0
 */
final class LifecycleMethodUtils {

	private LifecycleMethodUtils() {
		/* no-op */
	}

	static List<Method> findBeforeAllMethods(Class<?> testClass, boolean requireStatic,
			DiscoveryIssueReporter issueReporter) {
		return findMethodsAndCheckStatic(testClass, requireStatic, BeforeAll.class, HierarchyTraversalMode.TOP_DOWN,
			issueReporter);
	}

	static List<Method> findAfterAllMethods(Class<?> testClass, boolean requireStatic,
			DiscoveryIssueReporter issueReporter) {
		return findMethodsAndCheckStatic(testClass, requireStatic, AfterAll.class, HierarchyTraversalMode.BOTTOM_UP,
			issueReporter);
	}

	static List<Method> findBeforeEachMethods(Class<?> testClass, DiscoveryIssueReporter issueReporter) {
		return findMethodsAndCheckNonStatic(testClass, BeforeEach.class, HierarchyTraversalMode.TOP_DOWN,
			issueReporter);
	}

	static List<Method> findAfterEachMethods(Class<?> testClass, DiscoveryIssueReporter issueReporter) {
		return findMethodsAndCheckNonStatic(testClass, AfterEach.class, HierarchyTraversalMode.BOTTOM_UP,
			issueReporter);
	}

	static void validateNoClassTemplateInvocationLifecycleMethodsAreDeclared(Class<?> testClass,
			DiscoveryIssueReporter issueReporter) {

		findAllClassTemplateInvocationLifecycleMethods(testClass) //
				.forEach(method -> findClassTemplateInvocationLifecycleMethodAnnotation(method) //
						.ifPresent(annotation -> {
							String message = "@%s method '%s' must not be declared in test class '%s' because it is not annotated with @%s.".formatted(
								annotation.lifecycleMethodAnnotation().getSimpleName(), method.toGenericString(),
								testClass.getName(), annotation.classTemplateAnnotation().getSimpleName());
							issueReporter.reportIssue(createIssue(Severity.ERROR, message, method));
						}));
	}

	static void validateClassTemplateInvocationLifecycleMethodsAreDeclaredCorrectly(Class<?> testClass,
			boolean requireStatic, DiscoveryIssueReporter issueReporter) {

		findAllClassTemplateInvocationLifecycleMethods(testClass) //
				.forEach(isNotPrivateError(issueReporter) //
						.and(returnsPrimitiveVoid(issueReporter,
							LifecycleMethodUtils::classTemplateInvocationLifecycleMethodAnnotationName)) //
						.and(requireStatic
								? isStatic(issueReporter,
									LifecycleMethodUtils::classTemplateInvocationLifecycleMethodAnnotationName)
								: alwaysSatisfied()) //
						.toConsumer());
	}

	private static Stream<Method> findAllClassTemplateInvocationLifecycleMethods(Class<?> testClass) {
		Stream<Method> allMethods = Stream.concat( //
			findAnnotatedMethods(testClass, ClassTemplateInvocationLifecycleMethod.class,
				HierarchyTraversalMode.TOP_DOWN).stream(), //
			findAnnotatedMethods(testClass, ClassTemplateInvocationLifecycleMethod.class,
				HierarchyTraversalMode.BOTTOM_UP).stream() //
		);
		return allMethods.distinct();
	}

	private static List<Method> findMethodsAndCheckStatic(Class<?> testClass, boolean requireStatic,
			Class<? extends Annotation> annotationType, HierarchyTraversalMode traversalMode,
			DiscoveryIssueReporter issueReporter) {

		Condition<Method> additionalCondition = requireStatic
				? isStatic(issueReporter, __ -> annotationType.getSimpleName())
				: alwaysSatisfied();
		return findMethodsAndCheckVoidReturnType(testClass, annotationType, traversalMode, issueReporter,
			additionalCondition);
	}

	private static List<Method> findMethodsAndCheckNonStatic(Class<?> testClass,
			Class<? extends Annotation> annotationType, HierarchyTraversalMode traversalMode,
			DiscoveryIssueReporter issueReporter) {

		return findMethodsAndCheckVoidReturnType(testClass, annotationType, traversalMode, issueReporter,
			isNotStatic(issueReporter, __ -> annotationType.getSimpleName()));
	}

	private static List<Method> findMethodsAndCheckVoidReturnType(Class<?> testClass,
			Class<? extends Annotation> annotationType, HierarchyTraversalMode traversalMode,
			DiscoveryIssueReporter issueReporter, Condition<? super Method> additionalCondition) {

		return findAnnotatedMethods(testClass, annotationType, traversalMode).stream() //
				.peek(isNotPrivateWarning(issueReporter, annotationType::getSimpleName).toConsumer()) //
				.filter(returnsPrimitiveVoid(issueReporter, __ -> annotationType.getSimpleName()).and(
					additionalCondition).toPredicate()) //
				.toList();
	}

	private static Condition<Method> isStatic(DiscoveryIssueReporter issueReporter,
			Function<Method, String> annotationNameProvider) {
		return issueReporter.createReportingCondition(ModifierSupport::isStatic, method -> {
			String message = "@%s method '%s' must be static unless the test class is annotated with @TestInstance(Lifecycle.PER_CLASS).".formatted(
				annotationNameProvider.apply(method), method.toGenericString());
			return createIssue(Severity.ERROR, message, method);
		});
	}

	private static Condition<Method> isNotStatic(DiscoveryIssueReporter issueReporter,
			Function<Method, String> annotationNameProvider) {
		return issueReporter.createReportingCondition(ModifierSupport::isNotStatic, method -> {
			String message = "@%s method '%s' must not be static.".formatted(annotationNameProvider.apply(method),
				method.toGenericString());
			return createIssue(Severity.ERROR, message, method);
		});
	}

	private static Condition<Method> isNotPrivateError(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(ModifierSupport::isNotPrivate, method -> {
			String message = "@%s method '%s' must not be private.".formatted(
				classTemplateInvocationLifecycleMethodAnnotationName(method), method.toGenericString());
			return createIssue(Severity.ERROR, message, method);
		});
	}

	private static Condition<Method> isNotPrivateWarning(DiscoveryIssueReporter issueReporter,
			Supplier<String> annotationNameProvider) {
		return issueReporter.createReportingCondition(ModifierSupport::isNotPrivate, method -> {
			String message = "@%s method '%s' should not be private. This will be disallowed in a future release.".formatted(
				annotationNameProvider.get(), method.toGenericString());
			return createIssue(Severity.WARNING, message, method);
		});
	}

	private static Condition<Method> returnsPrimitiveVoid(DiscoveryIssueReporter issueReporter,
			Function<Method, String> annotationNameProvider) {
		return issueReporter.createReportingCondition(method -> getReturnType(method) == void.class, method -> {
			String message = "@%s method '%s' must not return a value.".formatted(annotationNameProvider.apply(method),
				method.toGenericString());
			return createIssue(Severity.ERROR, message, method);
		});
	}

	private static String classTemplateInvocationLifecycleMethodAnnotationName(Method method) {
		return findClassTemplateInvocationLifecycleMethodAnnotation(method) //
				.map(ClassTemplateInvocationLifecycleMethod::lifecycleMethodAnnotation) //
				.map(Class::getSimpleName) //
				.orElseGet(ClassTemplateInvocationLifecycleMethod.class::getSimpleName);
	}

	private static Optional<ClassTemplateInvocationLifecycleMethod> findClassTemplateInvocationLifecycleMethodAnnotation(
			Method method) {
		return findAnnotation(method, ClassTemplateInvocationLifecycleMethod.class);
	}

	private static DiscoveryIssue createIssue(Severity severity, String message, Method method) {
		return DiscoveryIssue.builder(severity, message).source(MethodSource.from(method)).build();
	}

}
