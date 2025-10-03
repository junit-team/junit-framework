/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullOrEmptyFor;
import static org.junit.platform.launcher.MethodFilter.excludeMethodNamePatterns;
import static org.junit.platform.launcher.MethodFilter.includeMethodNamePatterns;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.DemoMethodTestDescriptor;

/**
 * Unit tests for {@link MethodFilter}.
 *
 * @since 1.12
 */
class MethodFilterTests {
	private static final String CLASS1_TEST1_NAME = "org.junit.platform.launcher.MethodFilterTests$Class1#test1";
	private static final String CLASS1_TEST2_NAME = "org.junit.platform.launcher.MethodFilterTests$Class1#test2";
	private static final String CLASS2_TEST1_NAME = "org.junit.platform.launcher.MethodFilterTests$Class2#test1";
	private static final String CLASS2_TEST2_NAME = "org.junit.platform.launcher.MethodFilterTests$Class2#test2";
	private static final TestDescriptor CLASS1_TEST1 = methodTestDescriptor("class1", Class1.class, "test1");
	private static final TestDescriptor CLASS1_TEST2 = methodTestDescriptor("class1", Class1.class, "test2");
	private static final TestDescriptor CLASS2_TEST1 = methodTestDescriptor("class2", Class2.class, "test1");
	private static final TestDescriptor CLASS2_TEST2 = methodTestDescriptor("class2", Class2.class, "test2");

	@SuppressWarnings("DataFlowIssue")
	@Test
	void includeMethodNamePatternsChecksPreconditions() {
		assertPreconditionViolationNotNullOrEmptyFor("patterns array",
			() -> includeMethodNamePatterns((String[]) null));
		assertPreconditionViolationNotNullOrEmptyFor("patterns array", () -> includeMethodNamePatterns(new String[0]));
		assertPreconditionViolationFor(() -> includeMethodNamePatterns(new String[] { null })).withMessage(
			"patterns array must not contain null elements");
	}

	@Test
	void includeSingleMethodNamePattern() {
		var regex = "^org\\.junit\\.platform\\.launcher\\.MethodFilterTests\\$Class1#test.*";
		var filter = includeMethodNamePatterns(regex);

		assertIncluded(filter.apply(CLASS1_TEST1),
			"Method name [%s] matches included pattern: '%s'".formatted(CLASS1_TEST1_NAME, regex));
		assertIncluded(filter.apply(CLASS1_TEST2),
			"Method name [%s] matches included pattern: '%s'".formatted(CLASS1_TEST2_NAME, regex));

		assertExcluded(filter.apply(CLASS2_TEST1),
			"Method name [%s] does not match any included pattern: '%s'".formatted(CLASS2_TEST1_NAME, regex));
		assertExcluded(filter.apply(CLASS2_TEST2),
			"Method name [%s] does not match any included pattern: '%s'".formatted(CLASS2_TEST2_NAME, regex));
	}

	@Test
	void includeMultipleMethodNamePatterns() {
		var firstRegex = "^org\\.junit\\.platform\\.launcher\\.MethodFilterTests\\$Class1#test.*";
		var secondRegex = ".+Class.+#test1";
		var filter = includeMethodNamePatterns(firstRegex, secondRegex);

		assertIncluded(filter.apply(CLASS1_TEST1),
			"Method name [%s] matches included pattern: '%s'".formatted(CLASS1_TEST1_NAME, firstRegex));
		assertIncluded(filter.apply(CLASS1_TEST2),
			"Method name [%s] matches included pattern: '%s'".formatted(CLASS1_TEST2_NAME, firstRegex));
		assertIncluded(filter.apply(CLASS2_TEST1),
			"Method name [%s] matches included pattern: '%s'".formatted(CLASS2_TEST1_NAME, secondRegex));

		assertExcluded(filter.apply(CLASS2_TEST2),
			"Method name [%s] does not match any included pattern: '%s' OR '%s'".formatted(CLASS2_TEST2_NAME,
				firstRegex, secondRegex));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void excludeMethodNamePatternsChecksPreconditions() {
		assertPreconditionViolationNotNullOrEmptyFor("patterns array",
			() -> excludeMethodNamePatterns((String[]) null));
		assertPreconditionViolationNotNullOrEmptyFor("patterns array", () -> excludeMethodNamePatterns(new String[0]));
		assertPreconditionViolationFor(() -> excludeMethodNamePatterns(new String[] { null })).withMessage(
			"patterns array must not contain null elements");
	}

	@Test
	void excludeSingleMethodNamePattern() {
		var regex = "^org\\.junit\\.platform\\.launcher\\.MethodFilterTests\\$Class1#test.*";
		var filter = excludeMethodNamePatterns(regex);

		assertExcluded(filter.apply(CLASS1_TEST1),
			"Method name [%s] matches excluded pattern: '%s'".formatted(CLASS1_TEST1_NAME, regex));
		assertExcluded(filter.apply(CLASS1_TEST2),
			"Method name [%s] matches excluded pattern: '%s'".formatted(CLASS1_TEST2_NAME, regex));

		assertIncluded(filter.apply(CLASS2_TEST1),
			"Method name [%s] does not match any excluded pattern: '%s'".formatted(CLASS2_TEST1_NAME, regex));
		assertIncluded(filter.apply(CLASS2_TEST2),
			"Method name [%s] does not match any excluded pattern: '%s'".formatted(CLASS2_TEST2_NAME, regex));
	}

	@Test
	void excludeMultipleMethodNamePatterns() {
		var firstRegex = "^org\\.junit\\.platform\\.launcher\\.MethodFilterTests\\$Class1#test.*";
		var secondRegex = ".+Class.+#test1";
		var filter = excludeMethodNamePatterns(firstRegex, secondRegex);

		assertExcluded(filter.apply(CLASS1_TEST1),
			"Method name [%s] matches excluded pattern: '%s'".formatted(CLASS1_TEST1_NAME, firstRegex));
		assertExcluded(filter.apply(CLASS1_TEST2),
			"Method name [%s] matches excluded pattern: '%s'".formatted(CLASS1_TEST2_NAME, firstRegex));
		assertExcluded(filter.apply(CLASS2_TEST1),
			"Method name [%s] matches excluded pattern: '%s'".formatted(CLASS2_TEST1_NAME, secondRegex));

		assertIncluded(filter.apply(CLASS2_TEST2),
			"Method name [%s] does not match any excluded pattern: '%s' OR '%s'".formatted(CLASS2_TEST2_NAME,
				firstRegex, secondRegex));
	}

	private void assertIncluded(FilterResult filterResult, String expectedReason) {
		assertTrue(filterResult.included());
		assertThat(filterResult.getReason()).isPresent().contains(expectedReason);
	}

	private void assertExcluded(FilterResult filterResult, String excludedPattern) {
		assertTrue(filterResult.excluded());
		assertThat(filterResult.getReason()).isPresent().contains(excludedPattern);
	}

	private static TestDescriptor methodTestDescriptor(String uniqueId, Class<?> testClass, String methodName) {
		var method = ReflectionUtils.findMethod(testClass, methodName, new Class<?>[0]).orElseThrow();
		return new DemoMethodTestDescriptor(UniqueId.root("method", uniqueId), method);
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	private static class Class1 {
		@Test
		void test1() {
		}

		@Test
		void test2() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	private static class Class2 {
		@Test
		void test1() {
		}

		@Test
		void test2() {
		}
	}
}
