/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery.predicates;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;
import static org.junit.platform.commons.support.ModifierSupport.isAbstract;
import static org.junit.platform.commons.support.ModifierSupport.isNotAbstract;
import static org.junit.platform.commons.support.ModifierSupport.isNotPrivate;
import static org.junit.platform.commons.util.KotlinReflectionUtils.isKotlinInterfaceDefaultImplsClass;
import static org.junit.platform.commons.util.ReflectionUtils.isInnerClass;
import static org.junit.platform.commons.util.ReflectionUtils.isMethodPresent;
import static org.junit.platform.commons.util.ReflectionUtils.isNestedClassPresent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.Nested;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.CycleErrorHandling;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.Condition;

/**
 * Predicates for determining whether a class is a JUnit Jupiter test class.
 *
 * @since 5.13
 */
@API(status = INTERNAL, since = "5.13")
public class TestClassPredicates {

	public final Predicate<Class<?>> isAnnotatedWithNested = candidate -> isAnnotatedButNotComposed(candidate,
		Nested.class);
	public final Predicate<Class<?>> isAnnotatedWithClassTemplate = candidate -> isAnnotatedButNotComposed(candidate,
		ClassTemplate.class);

	public final Predicate<Class<?>> isAnnotatedWithNestedAndValid = candidate -> this.isAnnotatedWithNested.test(
		candidate) && isValidNestedTestClass(candidate);
	public final Predicate<Class<?>> looksLikeNestedOrStandaloneTestClass = candidate -> this.isAnnotatedWithNested.test(
		candidate) || looksLikeIntendedTestClass(candidate);
	public final Predicate<Method> isTestOrTestFactoryOrTestTemplateMethod;

	private final Condition<Class<?>> isNotPrivateUnlessAbstractNestedClass;
	private final Condition<Class<?>> isInnerNestedClass;
	private final Condition<Class<?>> isValidStandaloneTestClass;

	public TestClassPredicates(DiscoveryIssueReporter issueReporter) {
		this.isTestOrTestFactoryOrTestTemplateMethod = new IsTestMethod(issueReporter) //
				.or(new IsTestFactoryMethod(issueReporter)) //
				.or(new IsTestTemplateMethod(issueReporter));
		this.isNotPrivateUnlessAbstractNestedClass = isNotPrivateUnlessAbstract("@Nested", issueReporter);
		this.isInnerNestedClass = isInner(issueReporter);
		this.isValidStandaloneTestClass = isNotPrivateUnlessAbstract("Test", issueReporter) //
				.and(isNotLocal(issueReporter)) //
				.and(isNotInnerUnlessAbstract(issueReporter)) //
				.and(isNotAnonymous(issueReporter));
	}

	public boolean looksLikeIntendedTestClass(Class<?> candidate) {
		return looksLikeIntendedTestClass(candidate, new HashSet<>());
	}

	private boolean looksLikeIntendedTestClass(Class<?> candidate, Set<Class<?>> seen) {
		if (seen.add(candidate) && !isKotlinInterfaceDefaultImplsClass(candidate)) {
			return this.isAnnotatedWithClassTemplate.test(candidate) //
					|| hasTestOrTestFactoryOrTestTemplateMethods(candidate) //
					|| hasNestedTests(candidate, seen);
		}
		return false;
	}

	public boolean isValidNestedTestClass(Class<?> candidate) {
		return validateNestedTestClass(candidate) == null;
	}

	public @Nullable NestedClassInvalidityReason validateNestedTestClass(Class<?> candidate) {
		boolean isInner = this.isInnerNestedClass.check(candidate);
		boolean isNotPrivateUnlessAbstract = this.isNotPrivateUnlessAbstractNestedClass.check(candidate);
		if (isNotPrivateUnlessAbstract && isNotAbstract(candidate)) {
			return isInner ? null : NestedClassInvalidityReason.NOT_INNER;
		}
		return NestedClassInvalidityReason.OTHER;
	}

	public boolean isValidStandaloneTestClass(Class<?> candidate) {
		return this.isValidStandaloneTestClass.check(candidate) //
				&& isNotAbstract(candidate);
	}

	private boolean hasTestOrTestFactoryOrTestTemplateMethods(Class<?> candidate) {
		return isMethodPresent(candidate, this.isTestOrTestFactoryOrTestTemplateMethod);
	}

	private boolean hasNestedTests(Class<?> candidate, Set<Class<?>> seen) {
		var hasAnnotatedClass = isNestedClassPresent(candidate, this.isAnnotatedWithNested,
			CycleErrorHandling.THROW_EXCEPTION);
		if (hasAnnotatedClass) {
			return true;
		}
		return isNestedClassPresent( //
			candidate, //
			it -> isInnerClass(it) && looksLikeIntendedTestClass(it, seen), //
			CycleErrorHandling.ABORT_VISIT //
		);
	}

	private static Condition<Class<?>> isNotPrivateUnlessAbstract(String prefix, DiscoveryIssueReporter issueReporter) {
		// Allow abstract test classes to be private because subclasses may widen access.
		return issueReporter.createReportingCondition(testClass -> isNotPrivate(testClass) || isAbstract(testClass),
			testClass -> createIssue(prefix, testClass, "must not be private"));
	}

	private static Condition<Class<?>> isNotLocal(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(testClass -> !testClass.isLocalClass(),
			testClass -> createIssue("Test", testClass, "must not be a local class"));
	}

	private static Condition<Class<?>> isInner(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(ReflectionUtils::isInnerClass, testClass -> {
			if (testClass.getEnclosingClass() == null) {
				return createIssue("Top-level", testClass, "must not be annotated with @Nested",
					"It will be executed anyway for backward compatibility. "
							+ "You should remove the @Nested annotation to resolve this warning.");
			}
			return createIssue("@Nested", testClass, "must not be static",
				"It will only be executed if discovered as a standalone test class. "
						+ "You should remove the annotation or make it non-static to resolve this warning.");
		});
	}

	private static Condition<Class<?>> isNotInnerUnlessAbstract(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(testClass -> !isInnerClass(testClass) || isAbstract(testClass),
			testClass -> createIssue("Test", testClass, "must not be an inner class unless annotated with @Nested"));
	}

	private static Condition<Class<?>> isNotAnonymous(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(testClass -> !testClass.isAnonymousClass(),
			testClass -> createIssue("Test", testClass, "must not be anonymous"));
	}

	private static DiscoveryIssue createIssue(String prefix, Class<?> testClass, String detailMessage) {
		return createIssue(prefix, testClass, detailMessage, "It will not be executed.");
	}

	private static DiscoveryIssue createIssue(String prefix, Class<?> testClass, String detailMessage, String effect) {
		String message = "%s class '%s' %s. %s".formatted(prefix, testClass.getName(), detailMessage, effect);
		return DiscoveryIssue.builder(DiscoveryIssue.Severity.WARNING, message) //
				.source(ClassSource.from(testClass)) //
				.build();
	}

	private static boolean isAnnotatedButNotComposed(Class<?> candidate, Class<? extends Annotation> annotationType) {
		return !candidate.isAnnotation() && isAnnotated(candidate, annotationType);
	}

	@API(status = INTERNAL, since = "5.13.3")
	public enum NestedClassInvalidityReason {
		NOT_INNER, OTHER
	}
}
