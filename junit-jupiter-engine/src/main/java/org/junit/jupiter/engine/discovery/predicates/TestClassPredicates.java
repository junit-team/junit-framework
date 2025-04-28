/*
 * Copyright 2015-2025 the original author or authors.
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
import static org.junit.platform.commons.util.ReflectionUtils.isInnerClass;
import static org.junit.platform.commons.util.ReflectionUtils.isMethodPresent;
import static org.junit.platform.commons.util.ReflectionUtils.isNestedClassPresent;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.Nested;
import org.junit.platform.commons.util.ReflectionUtils;
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

	public final Predicate<Class<?>> isAnnotatedWithNested = testClass -> isAnnotated(testClass, Nested.class);
	public final Predicate<Class<?>> isAnnotatedWithClassTemplate = testClass -> isAnnotated(testClass,
		ClassTemplate.class);

	public final Predicate<Class<?>> isAnnotatedWithNestedAndValid = candidate -> this.isAnnotatedWithNested.test(
		candidate) && isValidNestedTestClass(candidate);
	public final Predicate<Class<?>> looksLikeNestedOrStandaloneTestClass = candidate -> this.isAnnotatedWithNested.test(
		candidate) || looksLikeIntendedTestClass(candidate);
	public final Predicate<Method> isTestOrTestFactoryOrTestTemplateMethod;

	private final Condition<Class<?>> isValidNestedTestClass;
	private final Condition<Class<?>> isValidStandaloneTestClass;

	public TestClassPredicates(DiscoveryIssueReporter issueReporter) {
		this.isTestOrTestFactoryOrTestTemplateMethod = new IsTestMethod(issueReporter) //
				.or(new IsTestFactoryMethod(issueReporter)) //
				.or(new IsTestTemplateMethod(issueReporter));
		this.isValidNestedTestClass = isNotPrivateUnlessAbstract("@Nested", issueReporter) //
				.and(isInner(issueReporter));
		this.isValidStandaloneTestClass = isNotPrivateUnlessAbstract("Test", issueReporter) //
				.and(isNotLocal(issueReporter)) //
				.and(isNotInner(issueReporter)) // or should be annotated with @Nested!
				.and(isNotAnonymous(issueReporter));
	}

	public boolean looksLikeIntendedTestClass(Class<?> candidate) {
		return this.isAnnotatedWithClassTemplate.test(candidate) //
				|| hasTestOrTestFactoryOrTestTemplateMethods(candidate) //
				|| hasNestedTests(candidate);
	}

	public boolean isValidNestedTestClass(Class<?> candidate) {
		return this.isValidNestedTestClass.check(candidate) //
				&& isNotAbstract(candidate);
	}

	public boolean isValidStandaloneTestClass(Class<?> candidate) {
		return this.isValidStandaloneTestClass.check(candidate) //
				&& isNotAbstract(candidate);
	}

	private boolean hasTestOrTestFactoryOrTestTemplateMethods(Class<?> candidate) {
		return isMethodPresent(candidate, this.isTestOrTestFactoryOrTestTemplateMethod);
	}

	private boolean hasNestedTests(Class<?> candidate) {
		return isNestedClassPresent( //
			candidate, //
			isNotSame(candidate).and(
				this.isAnnotatedWithNested.or(it -> isInnerClass(it) && looksLikeIntendedTestClass(it))));
	}

	private static Predicate<Class<?>> isNotSame(Class<?> candidate) {
		return clazz -> candidate != clazz;
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
				return createIssue("@Nested", testClass, "must not be a top-level class");
			}
			return createIssue("@Nested", testClass, "must not be static");
		});
	}

	private static Condition<Class<?>> isNotInner(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(testClass -> !isInnerClass(testClass),
			testClass -> createIssue("Test", testClass, "must not be an inner class unless annotated with @Nested"));
	}

	private static Condition<Class<?>> isNotAnonymous(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(testClass -> !testClass.isAnonymousClass(),
			testClass -> createIssue("Test", testClass, "must not be anonymous"));
	}

	private static DiscoveryIssue createIssue(String prefix, Class<?> testClass, String detailMessage) {
		String message = String.format("%s class '%s' %s. It will not be executed.", prefix, testClass.getName(),
			detailMessage);
		return DiscoveryIssue.builder(DiscoveryIssue.Severity.WARNING, message) //
				.source(ClassSource.from(testClass)) //
				.build();
	}
}
