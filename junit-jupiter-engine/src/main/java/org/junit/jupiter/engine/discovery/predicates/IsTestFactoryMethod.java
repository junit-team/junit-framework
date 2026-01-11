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
import static org.junit.jupiter.engine.support.MethodReflectionUtils.getGenericReturnType;
import static org.junit.jupiter.engine.support.MethodReflectionUtils.getReturnType;
import static org.junit.platform.commons.util.CollectionUtils.isConvertibleToStream;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import org.apiguardian.api.API;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

/**
 * Test if a method is a JUnit Jupiter {@link TestFactory @TestFactory} method.
 *
 * <p>NOTE: this predicate does <strong>not</strong> check if a candidate method
 * has an appropriate return type for a {@code @TestFactory} method.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class IsTestFactoryMethod extends IsTestableMethod {

	private static final String EXPECTED_RETURN_TYPE_MESSAGE = "must return a single %1$s or a Stream, Collection, Iterable, Iterator, Iterator provider, or array of %1$s".formatted(
		DynamicNode.class.getName());

	public IsTestFactoryMethod(DiscoveryIssueReporter issueReporter) {
		super(TestFactory.class, IsTestFactoryMethod::hasCompatibleReturnType, issueReporter);
	}

	private static DiscoveryIssueReporter.Condition<Method> hasCompatibleReturnType(
			Class<? extends Annotation> annotationType, DiscoveryIssueReporter issueReporter) {
		return issueReporter.createReportingCondition(method -> isCompatible(method, issueReporter),
			method -> createIssue(annotationType, method, EXPECTED_RETURN_TYPE_MESSAGE));
	}

	private static boolean isCompatible(Method method, DiscoveryIssueReporter issueReporter) {
		Class<?> returnType = getReturnType(method);
		if (DynamicNode.class.isAssignableFrom(returnType) || DynamicNode[].class.isAssignableFrom(returnType)) {
			return true;
		}
		if (returnType == Object.class || returnType == Object[].class) {
			issueReporter.reportIssue(createTooGenericReturnTypeIssue(method));
			return true;
		}
		boolean validContainerType = !returnType.isArray() && isConvertibleToStream(returnType);
		return validContainerType && isCompatibleContainerType(method, issueReporter);
	}

	private static boolean isCompatibleContainerType(Method method, DiscoveryIssueReporter issueReporter) {
		Type genericReturnType = getGenericReturnType(method);

		if (genericReturnType instanceof ParameterizedType type) {
			Type[] typeArguments = type.getActualTypeArguments();
			if (typeArguments.length == 1) {
				Type typeArgument = typeArguments[0];
				if (typeArgument instanceof Class<?> clazz) {
					// Stream<DynamicNode> etc.
					return DynamicNode.class.isAssignableFrom(clazz);
				}
				if (typeArgument instanceof WildcardType wildcardType) {
					Type[] upperBounds = wildcardType.getUpperBounds();
					Type[] lowerBounds = wildcardType.getLowerBounds();
					if (upperBounds.length == 1 && lowerBounds.length == 0
							&& upperBounds[0] instanceof Class<?> upperBound) {
						if (Object.class.equals(upperBound)) { // Stream<?> etc.
							issueReporter.reportIssue(createTooGenericReturnTypeIssue(method));
							return true;
						}
						// Stream<? extends DynamicNode> etc.
						return DynamicNode.class.isAssignableFrom(upperBound);
					}
				}
			}
			return false;
		}

		// Raw Stream etc. without type argument
		issueReporter.reportIssue(createTooGenericReturnTypeIssue(method));
		return true;
	}

	private static DiscoveryIssue.Builder createTooGenericReturnTypeIssue(Method method) {
		String message = ("The declared return type of @TestFactory method '%s' does not support static validation. It "
				+ EXPECTED_RETURN_TYPE_MESSAGE + ".").formatted(method.toGenericString());
		return DiscoveryIssue.builder(Severity.INFO, message) //
				.source(MethodSource.from(method));
	}

}
