/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.platform.testkit.engine.EventConditions.started;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Execute")
class JUnitJupiterKitTests {

	@Test
	@DisplayName("all tests of a class")
	void executeTestClass() {
		ExecutionResults results = JUnitJupiterTestKit.executeTestClass(DummyClass.class);

		results.testEvents().assertThatEvents().haveExactly(1, started());
	}

	@Test
	@DisplayName("all tests of all given classes")
	void executeTestClasses() {
		ExecutionResults results = JUnitJupiterTestKit.executeTestClasses(
			asList(DummyClass.class, SecondDummyClass.class));

		results.testEvents().assertThatEvents().haveExactly(2, started());
	}

	@Test
	@DisplayName("a specific method")
	void executeTestMethod() {
		ExecutionResults results = JUnitJupiterTestKit.executeTestMethod(DummyClass.class, "nothing");

		results.testEvents().assertThatEvents().haveExactly(1, started());
	}

	@Nested
	@DisplayName("a specific parametrized method")
	class ExecuteTestMethodWithParametersTests {

		@Test
		@DisplayName(" where parameter is a single class")
		void executeTestMethodWithParameterTypes_singleParameterType() {
			ExecutionResults results = JUnitJupiterTestKit.executeTestMethodWithParameterTypes(DummyPropertyClass.class,
				"single", String.class);

			results.testEvents().assertThatEvents().haveExactly(1, started());
		}

		@Test
		@DisplayName(" where parameter is an array of classes")
		void executeTestMethodWithParameterTypes_parameterTypeAsArray() {
			Class<?>[] classes = { String.class };

			ExecutionResults results = JUnitJupiterTestKit.executeTestMethodWithParameterTypes(DummyPropertyClass.class,
				"single", classes);

			results.testEvents().assertThatEvents().haveExactly(1, started());
		}

		@Test
		@DisplayName("without parameter results in IllegalArgumentException")
		void executeTestMethodWithParameterTypes_parameterArrayIsNull_NullPointerException() {
			assertThatThrownBy(() -> JUnitJupiterTestKit.executeTestMethodWithParameterTypes(DummyPropertyClass.class,
				"single", (Class<?>) null)).isInstanceOf(NullPointerException.class);
		}

		@Test
		@DisplayName("without parameter results in IllegalArgumentException")
		void executeTestMethodWithParameterTypes_singleParameterIsNull_IllegalArgumentException() {
			assertThatThrownBy(() -> JUnitJupiterTestKit.executeTestMethodWithParameterTypes(DummyPropertyClass.class,
				"single", (Class<?>[]) null)).isInstanceOf(IllegalArgumentException.class).hasMessage(
					"methodParameterTypes must not be null");
		}

	}

	static class DummyPropertyClass {

		@ParameterizedTest(name = "See if enabled with {0}")
		@ValueSource(strings = { "parameter" })
		void single(String reason) {
			// Do nothing
		}

	}

	static class DummyClass {

		@Test
		void nothing() {
			// Do nothing
		}

	}

	static class SecondDummyClass {

		@Test
		void nothing() {
			// Do nothing
		}

	}

}
