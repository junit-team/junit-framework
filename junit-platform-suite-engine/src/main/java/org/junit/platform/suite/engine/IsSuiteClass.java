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

import static org.junit.platform.commons.support.ModifierSupport.isAbstract;
import static org.junit.platform.commons.support.ModifierSupport.isNotAbstract;
import static org.junit.platform.commons.support.ModifierSupport.isNotPrivate;

import java.util.function.Predicate;

import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.Condition;
import org.junit.platform.suite.api.Suite;

/**
 * @since 1.8
 */
final class IsSuiteClass implements Predicate<Class<?>> {

	private final Condition<Class<?>> condition;

	IsSuiteClass(DiscoveryIssueReporter issueReporter) {
		this.condition = isNotPrivateUnlessAbstract(issueReporter) //
				.and(isNotLocal(issueReporter)) //
				.and(isNotInner(issueReporter));
	}

	@Override
	public boolean test(Class<?> testClass) {
		return hasSuiteAnnotation(testClass) //
				&& condition.check(testClass) //
				&& isNotAbstract(testClass);
	}

	private boolean hasSuiteAnnotation(Class<?> testClass) {
		return AnnotationSupport.isAnnotated(testClass, Suite.class);
	}

	private static Condition<Class<?>> isNotPrivateUnlessAbstract(DiscoveryIssueReporter issueReporter) {
		// Allow abstract test classes to be private because @Suite is inherited and subclasses may widen access.
		return issueReporter.createReportingCondition(testClass -> isNotPrivate(testClass) || isAbstract(testClass),
			testClass -> createIssue(testClass, "must not be private."));
	}

	private static Condition<Class<?>> isNotLocal(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(testClass -> !testClass.isLocalClass(),
			testClass -> createIssue(testClass, "must not be a local class."));
	}

	private static Condition<Class<?>> isNotInner(DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(testClass -> !ReflectionUtils.isInnerClass(testClass),
			testClass -> createIssue(testClass, "must not be an inner class. Did you forget to declare it static?"));
	}

	private static DiscoveryIssue createIssue(Class<?> testClass, String detailMessage) {
		String message = "@Suite class '%s' %s It will not be executed.".formatted(testClass.getName(), detailMessage);
		return DiscoveryIssue.builder(DiscoveryIssue.Severity.WARNING, message) //
				.source(ClassSource.from(testClass)) //
				.build();
	}

}
