/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedMethods;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;

/**
 * Collection of utilities for working with test lifecycle methods.
 *
 * @since 1.11
 */
final class LifecycleMethodUtils {

	private LifecycleMethodUtils() {
		/* no-op */
	}

	static List<Method> findBeforeSuiteMethods(Class<?> testClass, DiscoveryIssueReporter issueReporter) {
		return findMethodsAndCheckStaticAndNonPrivate(testClass, BeforeSuite.class, HierarchyTraversalMode.TOP_DOWN,
			issueReporter);
	}

	static List<Method> findAfterSuiteMethods(Class<?> testClass, DiscoveryIssueReporter issueReporter) {
		return findMethodsAndCheckStaticAndNonPrivate(testClass, AfterSuite.class, HierarchyTraversalMode.BOTTOM_UP,
			issueReporter);
	}

	private static List<Method> findMethodsAndCheckStaticAndNonPrivate(Class<?> testClass,
			Class<? extends Annotation> annotationType, HierarchyTraversalMode traversalMode,
			DiscoveryIssueReporter issueReporter) {

		return findAnnotatedMethods(testClass, annotationType, traversalMode).stream() //
				.filter(//
					returnsPrimitiveVoid(annotationType, issueReporter) //
							.and(isStatic(annotationType, issueReporter)) //
							.and(isNotPrivate(annotationType, issueReporter)) //
							.and(hasNoParameters(annotationType, issueReporter)) //
							.toPredicate()) //
				.toList();
	}

	private static DiscoveryIssueReporter.Condition<Method> isStatic(Class<? extends Annotation> annotationType,
			DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(ModifierSupport::isStatic, method -> {
			String message = "@%s method '%s' must be static.".formatted(annotationType.getSimpleName(),
				method.toGenericString());
			return createError(message, method);
		});
	}

	private static DiscoveryIssueReporter.Condition<Method> isNotPrivate(Class<? extends Annotation> annotationType,
			DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(ModifierSupport::isNotPrivate, method -> {
			String message = "@%s method '%s' must not be private.".formatted(annotationType.getSimpleName(),
				method.toGenericString());
			return createError(message, method);
		});
	}

	private static DiscoveryIssueReporter.Condition<Method> returnsPrimitiveVoid(
			Class<? extends Annotation> annotationType, DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(ReflectionUtils::returnsPrimitiveVoid, method -> {
			String message = "@%s method '%s' must not return a value.".formatted(annotationType.getSimpleName(),
				method.toGenericString());
			return createError(message, method);
		});
	}

	private static DiscoveryIssueReporter.Condition<Method> hasNoParameters(Class<? extends Annotation> annotationType,
			DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(method -> method.getParameterCount() == 0, method -> {
			String message = "@%s method '%s' must not accept parameters.".formatted(annotationType.getSimpleName(),
				method.toGenericString());
			return createError(message, method);
		});
	}

	private static DiscoveryIssue createError(String message, Method method) {
		return DiscoveryIssue.builder(Severity.ERROR, message).source(MethodSource.from(method)).build();
	}

}
