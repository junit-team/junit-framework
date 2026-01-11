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

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;

/**
 * Unit tests for {@link IsTestMethod}.
 *
 * @since 5.0
 */
class IsTestMethodTests {

	final List<DiscoveryIssue> discoveryIssues = new ArrayList<>();
	final Predicate<Method> isTestMethod = new IsTestMethod(DiscoveryIssueReporter.collecting(discoveryIssues));

	@Test
	void publicTestMethod() {
		Method method = method("publicTestMethod");
		// Ensure that somebody doesn't accidentally delete the public modifier again.
		assertTrue(ModifierSupport.isPublic(method));
		assertThat(isTestMethod).accepts(method);
	}

	@Test
	void publicTestMethodWithArgument() {
		Method method = method("publicTestMethodWithArgument", TestInfo.class);
		// Ensure that somebody doesn't accidentally delete the public modifier again.
		assertTrue(ModifierSupport.isPublic(method));
		assertThat(isTestMethod).accepts(method);
	}

	@Test
	void protectedTestMethod() {
		assertThat(isTestMethod).accepts(method("protectedTestMethod"));
	}

	@Test
	void packageVisibleTestMethod() {
		assertThat(isTestMethod).accepts(method("packageVisibleTestMethod"));
	}

	@Test
	void bogusAbstractTestMethod() {
		var method = abstractMethod("bogusAbstractTestMethod");

		assertThat(isTestMethod).rejects(method);
		assertThat(discoveryIssues).isEmpty();
	}

	@Test
	void bogusAbstractNonVoidTestMethod() {
		var method = abstractMethod("bogusAbstractNonVoidTestMethod");

		assertThat(isTestMethod).rejects(method);

		var issue = getOnlyElement(discoveryIssues);
		assertThat(issue.message()) //
				.isEqualTo("@Test method '%s' must not return a value. It will not be executed.",
					method.toGenericString());
	}

	@Test
	void bogusStaticTestMethod() {
		var method = method("bogusStaticTestMethod");

		assertThat(isTestMethod).rejects(method);

		var issue = getOnlyElement(discoveryIssues);
		assertThat(issue.severity()).isEqualTo(Severity.WARNING);
		assertThat(issue.message()).isEqualTo("@Test method '%s' must not be static. It will not be executed.",
			method.toGenericString());
		assertThat(issue.source()).contains(MethodSource.from(method));
	}

	@Test
	void bogusPrivateTestMethod() {
		var method = method("bogusPrivateTestMethod");

		assertThat(isTestMethod).rejects(method);

		var issue = getOnlyElement(discoveryIssues);
		assertThat(issue.severity()).isEqualTo(Severity.WARNING);
		assertThat(issue.message()).isEqualTo("@Test method '%s' must not be private. It will not be executed.",
			method.toGenericString());
		assertThat(issue.source()).contains(MethodSource.from(method));
	}

	@ParameterizedTest
	@ValueSource(strings = { "bogusTestMethodReturningObject", "bogusTestMethodReturningVoidReference",
			"bogusTestMethodReturningPrimitive" })
	void bogusNonVoidTestMethods(String methodName) {
		var method = method(methodName);

		assertThat(isTestMethod).rejects(method);

		var issue = getOnlyElement(discoveryIssues);
		assertThat(issue.severity()).isEqualTo(Severity.WARNING);
		assertThat(issue.message()).isEqualTo("@Test method '%s' must not return a value. It will not be executed.",
			method.toGenericString());
		assertThat(issue.source()).contains(MethodSource.from(method));
	}

	@Test
	void bogusStaticPrivateNonVoidTestMethod() {
		var method = method("bogusStaticPrivateNonVoidTestMethod");

		assertThat(isTestMethod).rejects(method);

		assertThat(discoveryIssues).hasSize(3);
		discoveryIssues.sort(comparing(DiscoveryIssue::message));
		assertThat(discoveryIssues.getFirst().message()) //
				.isEqualTo("@Test method '%s' must not be private. It will not be executed.", method.toGenericString());
		assertThat(discoveryIssues.get(1).message()) //
				.isEqualTo("@Test method '%s' must not be static. It will not be executed.", method.toGenericString());
		assertThat(discoveryIssues.getLast().message()) //
				.isEqualTo("@Test method '%s' must not return a value. It will not be executed.",
					method.toGenericString());
	}

	private static Method method(String name, Class<?>... parameterTypes) {
		return ReflectionSupport.findMethod(ClassWithTestMethods.class, name, parameterTypes).orElseThrow();
	}

	private Method abstractMethod(String name) {
		return ReflectionSupport.findMethod(AbstractClassWithAbstractTestMethod.class, name).orElseThrow();
	}

	@SuppressWarnings({ "JUnitMalformedDeclaration", "unused" })
	private static abstract class AbstractClassWithAbstractTestMethod {

		@Test
		abstract void bogusAbstractTestMethod();

		@Test
		abstract int bogusAbstractNonVoidTestMethod();

	}

	@SuppressWarnings({ "JUnitMalformedDeclaration", "unused" })
	private static class ClassWithTestMethods {

		@Test
		static void bogusStaticTestMethod() {
		}

		@Test
		private void bogusPrivateTestMethod() {
		}

		@Test
		private static int bogusStaticPrivateNonVoidTestMethod() {
			return 42;
		}

		@Test
		String bogusTestMethodReturningObject() {
			return "";
		}

		@Test
		Void bogusTestMethodReturningVoidReference() {
			return null;
		}

		@Test
		int bogusTestMethodReturningPrimitive() {
			return 0;
		}

		@Test
		public void publicTestMethod() {
		}

		@Test
		public void publicTestMethodWithArgument(TestInfo info) {
		}

		@Test
		protected void protectedTestMethod() {
		}

		@Test
		void packageVisibleTestMethod() {
		}

	}

}
