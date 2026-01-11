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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

/**
 * Unit tests for {@link IsTestFactoryMethod}.
 *
 * @since 5.0
 */
class IsTestFactoryMethodTests {

	final List<DiscoveryIssue> discoveryIssues = new ArrayList<>();
	final Predicate<Method> isTestFactoryMethod = new IsTestFactoryMethod(
		DiscoveryIssueReporter.collecting(discoveryIssues));

	@ParameterizedTest
	@ValueSource(strings = { "dynamicTestsFactoryFromCollection", "dynamicTestsFactoryFromStreamWithExtendsWildcard",
			"dynamicTestsFactoryFromNode", "dynamicTestsFactoryFromTest", "dynamicTestsFactoryFromContainer",
			"dynamicTestsFactoryFromNodeArray", "dynamicTestsFactoryFromTestArray",
			"dynamicTestsFactoryFromContainerArray" })
	void validFactoryMethods(String methodName) {
		assertThat(isTestFactoryMethod).accepts(method(methodName));
		assertThat(discoveryIssues).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "bogusVoidFactory", "bogusStringsFactory", "bogusStringArrayFactory",
			"dynamicTestsFactoryFromStreamWithSuperWildcard" })
	void invalidFactoryMethods(String methodName) {
		var method = method(methodName);

		assertThat(isTestFactoryMethod).rejects(method);

		var issue = getOnlyElement(discoveryIssues);
		assertThat(issue.severity()).isEqualTo(DiscoveryIssue.Severity.WARNING);
		assertThat(issue.message()).isEqualTo(
			"@TestFactory method '%s' must return a single org.junit.jupiter.api.DynamicNode or a "
					+ "Stream, Collection, Iterable, Iterator, Iterator provider, or array of org.junit.jupiter.api.DynamicNode. "
					+ "It will not be executed.",
			method.toGenericString());
		assertThat(issue.source()).contains(MethodSource.from(method));
	}

	@ParameterizedTest
	@ValueSource(strings = { "objectFactory", "objectArrayFactory", "rawCollectionFactory", "unboundStreamFactory" })
	void suspiciousFactoryMethods(String methodName) {
		var method = method(methodName);

		assertThat(isTestFactoryMethod).accepts(method);

		var issue = getOnlyElement(discoveryIssues);
		assertThat(issue.severity()).isEqualTo(DiscoveryIssue.Severity.INFO);
		assertThat(issue.message()).isEqualTo(
			"The declared return type of @TestFactory method '%s' does not support static validation. "
					+ "It must return a single org.junit.jupiter.api.DynamicNode or a "
					+ "Stream, Collection, Iterable, Iterator, Iterator provider, or array of org.junit.jupiter.api.DynamicNode.",
			method.toGenericString());
		assertThat(issue.source()).contains(MethodSource.from(method));
	}

	private static Method method(String name) {
		return ReflectionSupport.findMethod(ClassWithTestFactoryMethods.class, name).orElseThrow();
	}

	@SuppressWarnings("unused")
	private static class ClassWithTestFactoryMethods {

		@TestFactory
		Collection<DynamicTest> dynamicTestsFactoryFromCollection() {
			return new ArrayList<>();
		}

		@TestFactory
		Stream<? extends DynamicTest> dynamicTestsFactoryFromStreamWithExtendsWildcard() {
			return Stream.empty();
		}

		@TestFactory
		DynamicTest dynamicTestsFactoryFromNode() {
			return dynamicTest("foo", Assertions::fail);
		}

		@TestFactory
		DynamicTest dynamicTestsFactoryFromTest() {
			return dynamicTest("foo", Assertions::fail);
		}

		@TestFactory
		DynamicNode dynamicTestsFactoryFromContainer() {
			return dynamicContainer("foo", Stream.empty());
		}

		@TestFactory
		DynamicNode[] dynamicTestsFactoryFromNodeArray() {
			return new DynamicNode[0];
		}

		@TestFactory
		DynamicTest[] dynamicTestsFactoryFromTestArray() {
			return new DynamicTest[0];
		}

		@TestFactory
		DynamicContainer[] dynamicTestsFactoryFromContainerArray() {
			return new DynamicContainer[0];
		}

		@TestFactory
		void bogusVoidFactory() {
		}

		@TestFactory
		Collection<String> bogusStringsFactory() {
			return new ArrayList<>();
		}

		@TestFactory
		String[] bogusStringArrayFactory() {
			return new String[0];
		}

		@TestFactory
		Stream<? super DynamicTest> dynamicTestsFactoryFromStreamWithSuperWildcard() {
			return Stream.empty();
		}

		@TestFactory
		Object objectFactory() {
			return dynamicTest("foo", Assertions::fail);
		}

		@TestFactory
		Object[] objectArrayFactory() {
			return new DynamicNode[0];
		}

		@SuppressWarnings("rawtypes")
		@TestFactory
		Collection rawCollectionFactory() {
			return new ArrayList<>();
		}

		@TestFactory
		Stream<?> unboundStreamFactory() {
			return Stream.of();
		}

	}

}
