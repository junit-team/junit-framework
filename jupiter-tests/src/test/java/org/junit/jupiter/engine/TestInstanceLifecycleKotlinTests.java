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
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.engine.kotlin.InstancePerClassKotlinTestCase;
import org.junit.jupiter.engine.kotlin.InstancePerMethodKotlinTestCase;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Kotlin-specific integration tests for {@link TestInstance @TestInstance}
 * lifecycle support.
 *
 * @since 5.1
 * @see TestInstanceLifecycleConfigurationTests
 * @see TestInstanceLifecycleTests
 */
class TestInstanceLifecycleKotlinTests extends AbstractJupiterTestEngineTests {

	@Test
	void instancePerClassCanBeUsedForKotlinTestClasses() {
		Class<?> testClass = InstancePerClassKotlinTestCase.class;
		InstancePerClassKotlinTestCase.TEST_INSTANCES.clear();

		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertThat(executionResults.testEvents().finished().count()).isEqualTo(2);
		assertThat(InstancePerClassKotlinTestCase.TEST_INSTANCES.keySet()).hasSize(1);
		assertThat(getOnlyElement(InstancePerClassKotlinTestCase.TEST_INSTANCES.values())) //
				.containsEntry("beforeAll", 1) //
				.containsEntry("beforeEach", 2) //
				.containsEntry("test", 2) //
				.containsEntry("afterEach", 2) //
				.containsEntry("afterAll", 1);
	}

	@Test
	void instancePerMethodIsDefaultForKotlinTestClasses() {
		Class<?> testClass = InstancePerMethodKotlinTestCase.class;
		InstancePerMethodKotlinTestCase.TEST_INSTANCES.clear();

		EngineExecutionResults executionResults = executeTestsForClass(testClass);

		assertThat(executionResults.testEvents().finished().count()).isEqualTo(2);
		List<Object> instances = new ArrayList<>(InstancePerMethodKotlinTestCase.TEST_INSTANCES.keySet());
		assertThat(instances) //
				.hasSize(3) //
				.extracting(o -> (Object) o.getClass()) //
				.containsExactly(InstancePerMethodKotlinTestCase.Companion.getClass(), //
					InstancePerMethodKotlinTestCase.class, //
					InstancePerMethodKotlinTestCase.class);
		assertThat(InstancePerMethodKotlinTestCase.TEST_INSTANCES.get(instances.get(0))) //
				.containsEntry("beforeAll", 1) //
				.containsEntry("afterAll", 1);
		assertThat(InstancePerMethodKotlinTestCase.TEST_INSTANCES.get(instances.get(1))) //
				.containsEntry("beforeEach", 1) //
				.containsEntry("test", 1) //
				.containsEntry("afterEach", 1);
		assertThat(InstancePerMethodKotlinTestCase.TEST_INSTANCES.get(instances.get(2))) //
				.containsEntry("beforeEach", 1) //
				.containsEntry("test", 1) //
				.containsEntry("afterEach", 1);
	}

}
