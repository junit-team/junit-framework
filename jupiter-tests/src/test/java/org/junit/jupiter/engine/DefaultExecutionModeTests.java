/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.CONCURRENT;
import static org.junit.platform.engine.support.hierarchical.Node.ExecutionMode.SAME_THREAD;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Constants;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;
import org.junit.platform.engine.support.hierarchical.Node.ExecutionMode;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

class DefaultExecutionModeTests extends AbstractJupiterTestEngineTests {

	@Test
	void defaultExecutionModeIsReadFromConfigurationParameter() {
		assertUsesExpectedExecutionMode(null, SAME_THREAD);
		assertUsesExpectedExecutionMode(SAME_THREAD, SAME_THREAD);
		assertUsesExpectedExecutionMode(CONCURRENT, CONCURRENT);
	}

	private void assertUsesExpectedExecutionMode(@Nullable ExecutionMode defaultExecutionMode,
			ExecutionMode expectedExecutionMode) {
		var engineDescriptor = discoverTestsWithDefaultExecutionMode(TestCase.class, defaultExecutionMode);
		assertExecutionModeRecursively(engineDescriptor, expectedExecutionMode);
	}

	@Test
	void annotationOverridesDefaultExecutionModeToConcurrentForAllDescendants() {
		assertUsesExpectedExecutionModeForTestClassAndItsDescendants(ConcurrentTestCase.class, null, CONCURRENT);
		assertUsesExpectedExecutionModeForTestClassAndItsDescendants(ConcurrentTestCase.class, SAME_THREAD, CONCURRENT);
		assertUsesExpectedExecutionModeForTestClassAndItsDescendants(ConcurrentTestCase.class, CONCURRENT, CONCURRENT);
	}

	@Test
	void annotationOverridesDefaultExecutionModeToSameThreadForAllDescendants() {
		assertUsesExpectedExecutionModeForTestClassAndItsDescendants(SameThreadTestCase.class, null, SAME_THREAD);
		assertUsesExpectedExecutionModeForTestClassAndItsDescendants(SameThreadTestCase.class, SAME_THREAD,
			SAME_THREAD);
		assertUsesExpectedExecutionModeForTestClassAndItsDescendants(SameThreadTestCase.class, CONCURRENT, SAME_THREAD);
	}

	private void assertUsesExpectedExecutionModeForTestClassAndItsDescendants(Class<?> testClass,
			@Nullable ExecutionMode defaultExecutionMode, ExecutionMode expectedExecutionMode) {
		var engineDescriptor = discoverTestsWithDefaultExecutionMode(testClass, defaultExecutionMode);
		engineDescriptor.getChildren().forEach(child -> assertExecutionModeRecursively(child, expectedExecutionMode));
	}

	private void assertExecutionModeRecursively(TestDescriptor testDescriptor, ExecutionMode expectedExecutionMode) {
		assertExecutionMode(testDescriptor, expectedExecutionMode);
		testDescriptor.getChildren().forEach(child -> assertExecutionModeRecursively(child, expectedExecutionMode));
	}

	@Test
	void methodsInTestClassesWithInstancePerClassHaveExecutionModeSameThread() {
		var engineDescriptor = discoverTestsWithDefaultExecutionMode(SimpleTestInstancePerClassTestCase.class,
			CONCURRENT);
		var classDescriptor = getOnlyElement(engineDescriptor.getChildren());
		classDescriptor.getChildren().forEach(child -> assertExecutionModeRecursively(child, SAME_THREAD));
	}

	@Test
	void methodsInNestedTestClassesWithInstancePerClassInHierarchyHaveExecutionModeSameThread() {
		var engineDescriptor = discoverTestsWithDefaultExecutionMode(OuterTestCase.class, CONCURRENT);
		var outerTestCaseClassDescriptor = firstChild(engineDescriptor, ClassTestDescriptor.class);
		var outerTestMethodDescriptor = firstChild(outerTestCaseClassDescriptor, TestMethodTestDescriptor.class);
		var level1NestedClassDescriptor = firstChild(outerTestCaseClassDescriptor, NestedClassTestDescriptor.class);
		var level1TestMethodDescriptor = firstChild(level1NestedClassDescriptor, TestMethodTestDescriptor.class);
		var level2NestedClassDescriptor = firstChild(level1NestedClassDescriptor, NestedClassTestDescriptor.class);
		var level2TestMethodDescriptor = firstChild(level2NestedClassDescriptor, TestMethodTestDescriptor.class);
		var level3NestedClassDescriptor = firstChild(level2NestedClassDescriptor, NestedClassTestDescriptor.class);
		var level3TestMethodDescriptor = firstChild(level3NestedClassDescriptor, TestMethodTestDescriptor.class);

		assertExecutionMode(outerTestCaseClassDescriptor, CONCURRENT);
		assertExecutionMode(outerTestMethodDescriptor, CONCURRENT);
		assertExecutionMode(level1NestedClassDescriptor, CONCURRENT);
		assertExecutionMode(level1TestMethodDescriptor, CONCURRENT);
		assertExecutionMode(level2NestedClassDescriptor, CONCURRENT);
		assertExecutionMode(level2TestMethodDescriptor, SAME_THREAD);
		assertExecutionMode(level3NestedClassDescriptor, SAME_THREAD);
		assertExecutionMode(level3TestMethodDescriptor, SAME_THREAD);
	}

	private JupiterEngineDescriptor discoverTestsWithDefaultExecutionMode(Class<?> testClass,
			@Nullable ExecutionMode executionMode) {
		LauncherDiscoveryRequestBuilder request = request().selectors(selectClass(testClass));
		if (executionMode != null) {
			request.configurationParameter(Constants.DEFAULT_EXECUTION_MODE_PROPERTY_NAME, executionMode.name());
		}
		return (JupiterEngineDescriptor) discoverTests(request.build()).getEngineDescriptor();
	}

	private static void assertExecutionMode(TestDescriptor testDescriptor, ExecutionMode expectedExecutionMode) {
		assertThat(((Node<?>) testDescriptor).getExecutionMode()) //
				.describedAs("ExecutionMode for %s", testDescriptor) //
				.isEqualTo(expectedExecutionMode);
	}

	@SuppressWarnings("unchecked")
	private <T extends TestDescriptor> T firstChild(TestDescriptor engineDescriptor, Class<T> testDescriptorClass) {
		return (T) engineDescriptor.getChildren().stream() //
				.filter(testDescriptorClass::isInstance) //
				.findFirst() //
				.orElseGet(() -> fail("No child of type " + testDescriptorClass + " found"));
	}

	static class TestCase {

		@Test
		void test() {
		}

		@Nested
		class NestedTestCase {

			@Test
			void test() {
			}

		}

	}

	@TestInstance(PER_CLASS)
	static class SimpleTestInstancePerClassTestCase extends TestCase {
	}

	@Execution(org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT)
	static class ConcurrentTestCase extends TestCase {
	}

	@Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
	static class SameThreadTestCase extends TestCase {
	}

	static class OuterTestCase {
		@Nested
		class LevelOne {
			@Nested
			@TestInstance(PER_CLASS)
			class LevelTwo {
				@Nested
				class LevelThree {
					@Test
					void test() {
					}
				}

				@Test
				void test() {
				}
			}

			@Test
			void test() {
			}
		}

		@Test
		void test() {
		}
	}

}
