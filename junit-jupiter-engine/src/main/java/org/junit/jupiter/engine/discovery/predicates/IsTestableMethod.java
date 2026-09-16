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

import static org.junit.jupiter.engine.support.MethodReflectionUtils.getReturnType;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;
import static org.junit.platform.commons.support.ModifierSupport.isNotAbstract;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.AsyncReturnValueHandler;
import org.junit.jupiter.engine.support.AsyncReturnTypeSupport;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.Condition;

/**
 * @since 5.0
 */
abstract class IsTestableMethod implements Predicate<Method> {

	private final Class<? extends Annotation> annotationType;
	private final Condition<Method> condition;

	IsTestableMethod(Class<? extends Annotation> annotationType,
			BiFunction<Class<? extends Annotation>, DiscoveryIssueReporter, Condition<Method>> returnTypeConditionFactory,
			DiscoveryIssueReporter issueReporter) {
		this.annotationType = annotationType;
		this.condition = isNotStatic(annotationType, issueReporter) //
				.and(isNotPrivate(annotationType, issueReporter)) //
				.and(returnTypeConditionFactory.apply(annotationType, issueReporter));
	}

	@Override
	public boolean test(Method candidate) {
		if (!candidate.isSynthetic() && isAnnotated(candidate, this.annotationType)) {
			return condition.check(candidate) && isNotAbstract(candidate);
		}
		return false;
	}

	private static Condition<Method> isNotStatic(Class<? extends Annotation> annotationType,
			DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(ModifierSupport::isNotStatic,
			method -> createIssue(annotationType, method, "must not be static"));
	}

	private static Condition<Method> isNotPrivate(Class<? extends Annotation> annotationType,
			DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(ModifierSupport::isNotPrivate,
			method -> createIssue(annotationType, method, "must not be private"));
	}

	protected static Condition<Method> hasVoidReturnType(Class<? extends Annotation> annotationType,
			DiscoveryIssueReporter issueReporter, List<AsyncReturnValueHandler> asyncReturnValueHandlers) {
		return issueReporter.createReportingCondition(
			method -> isVoidOrAsynchronousReturnType(method, asyncReturnValueHandlers),
			method -> createIssue(annotationType, method, "must not return a value"));
	}

	/**
	 * A test method may return {@code void} or a fully supported asynchronous
	 * return type (for example a {@link CompletionStage} or {@link Future}), in
	 * which case its completion is awaited before the test is considered
	 * finished.
	 *
	 * @see AsyncReturnTypeSupport
	 */
	static boolean isVoidOrAsynchronousReturnType(Method method,
			List<AsyncReturnValueHandler> asyncReturnValueHandlers) {
		Class<?> returnType = getReturnType(method);
		return returnType == void.class || AsyncReturnTypeSupport.isSupported(method, asyncReturnValueHandlers);
	}

	protected static DiscoveryIssue createIssue(Class<? extends Annotation> annotationType, Method method,
			String condition) {
		String message = "@%s method '%s' %s. It will not be executed.".formatted(annotationType.getSimpleName(),
			method.toGenericString(), condition);
		return DiscoveryIssue.builder(Severity.WARNING, message) //
				.source(MethodSource.from(method)) //
				.build();
	}

}
