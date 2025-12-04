/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.execution.injection.sample.CustomAnnotation;
import org.junit.jupiter.engine.execution.injection.sample.CustomAnnotationParameterResolver;
import org.junit.jupiter.engine.execution.injection.sample.CustomType;
import org.junit.jupiter.engine.execution.injection.sample.CustomTypeParameterResolver;
import org.junit.jupiter.engine.execution.injection.sample.MapOfListsTypeBasedParameterResolver;
import org.junit.jupiter.engine.execution.injection.sample.MapOfStringsParameterResolver;
import org.junit.jupiter.engine.execution.injection.sample.NullIntegerParameterResolver;
import org.junit.jupiter.engine.execution.injection.sample.NumberParameterResolver;
import org.junit.jupiter.engine.execution.injection.sample.PrimitiveArrayParameterResolver;
import org.junit.jupiter.engine.execution.injection.sample.PrimitiveIntegerParameterResolver;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Events;

/**
 * Integration tests that verify support for {@link ParameterResolver}
 * extensions in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class ParameterResolverTests extends AbstractJupiterTestEngineTests {

	@Test
	void constructorInjection() {
		EngineExecutionResults executionResults = executeTestsForClass(ConstructorInjectionTestCase.class);

		assertEquals(2, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(2, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");
	}

	@Test
	void constructorInjectionWithAnnotatedParameter() {
		EngineExecutionResults executionResults = executeTestsForClass(
			AnnotatedParameterConstructorInjectionTestCase.class);

		assertEquals(2, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(2, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");
	}

	@Test
	void executeTestsForMethodInjectionCases() {
		EngineExecutionResults executionResults = executeTestsForClass(MethodInjectionTestCase.class);

		assertEquals(7, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(6, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests failed");
	}

	@Test
	void executeTestsForNullValuedMethodInjectionCases() {
		EngineExecutionResults executionResults = executeTestsForClass(NullMethodInjectionTestCase.class);
		Events tests = executionResults.testEvents();

		assertEquals(2, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		Predicate<String> expectations = s ->
				s.contains("NullIntegerParameterResolver") &&
				s.contains("resolved a null value for parameter") &&
				s.contains("but a primitive of type [int] is required");

		tests.failed().assertEventsMatchExactly(
			event(
				test("injectPrimitive"),
				finishedWithFailure(instanceOf(ParameterResolutionException.class), message(expectations))
			));
		// @formatter:on
	}

	@Test
	void executeTestsForPrimitiveIntegerMethodInjectionCases() {
		EngineExecutionResults executionResults = executeTestsForClass(PrimitiveIntegerMethodInjectionTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");
	}

	@Test
	void executeTestsForPrimitiveArrayMethodInjectionCases() {
		EngineExecutionResults executionResults = executeTestsForClass(PrimitiveArrayMethodInjectionTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");
	}

	@Test
	void executeTestsForPotentiallyIncompatibleTypeMethodInjectionCases() {
		EngineExecutionResults executionResults = executeTestsForClass(
			PotentiallyIncompatibleTypeMethodInjectionTestCase.class);
		Events tests = executionResults.testEvents();

		assertEquals(3, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(2, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(1, executionResults.testEvents().failed().count(), "# tests failed");

		// @formatter:off
		Predicate<String> expectations = s ->
				s.contains("NumberParameterResolver") &&
				s.contains("resolved a value of type [java.lang.Integer]") &&
				s.contains("but a value assignment compatible with [java.lang.Double] is required");

		tests.failed().assertEventsMatchExactly(
			event(
				test("doubleParameterInjection"),
				finishedWithFailure(instanceOf(ParameterResolutionException.class), message(expectations)
			)));
		// @formatter:on
	}

	@Test
	void executeTestsForMethodInjectionInBeforeAndAfterEachMethods() {
		EngineExecutionResults executionResults = executeTestsForClass(BeforeAndAfterMethodInjectionTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");
	}

	@Test
	void executeTestsForMethodInjectionInBeforeAndAfterAllMethods() {
		EngineExecutionResults executionResults = executeTestsForClass(BeforeAndAfterAllMethodInjectionTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");
	}

	@Test
	void executeTestsForMethodWithExtendWithAnnotation() {
		EngineExecutionResults executionResults = executeTestsForClass(ExtendWithOnMethodTestCase.class);

		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");
	}

	@Test
	void executeTestsForParameterizedTypesSelectingByClass() {
		assertEventsForParameterizedTypes(executeTestsForClass(ParameterizedTypeTestCase.class));
	}

	@Test
	void executeTestsForParameterizedTypesSelectingByFullyQualifiedMethodName() {
		String fqmn = ReflectionUtils.getFullyQualifiedMethodName(ParameterizedTypeTestCase.class, "testMapOfStrings",
			Map.class);

		assertEventsForParameterizedTypes(executeTests(selectMethod(fqmn)));
	}

	@Test
	void executeTestsForTypeBasedParameterResolverTestCaseSelectingByClass() {
		assertEventsForParameterizedTypes(executeTestsForClass(TypeBasedParameterResolverTestCase.class));
	}

	@Test
	void executeTestsForTypeBasedParameterResolverTestCaseSelectingByFullyQualifiedMethodName() {
		String fqmn = ReflectionUtils.getFullyQualifiedMethodName(TypeBasedParameterResolverTestCase.class,
			"testMapOfLists", Map.class);

		assertEventsForParameterizedTypes(executeTests(selectMethod(fqmn)));
	}

	@Disabled("Disabled until a decision has been made regarding #956")
	@Test
	void executeTestsForParameterizedTypesSelectingByFullyQualifiedMethodNameContainingGenericInfo() throws Exception {
		Method method = ParameterizedTypeTestCase.class.getDeclaredMethod("testMapOfStrings", Map.class);
		String genericParameterTypeName = method.getGenericParameterTypes()[0].getTypeName();
		String fqmn = "%s#%s(%s)".formatted(ParameterizedTypeTestCase.class.getName(), "testMapOfStrings",
			genericParameterTypeName);

		assertEventsForParameterizedTypes(executeTests(selectMethod(fqmn)));
	}

	private void assertEventsForParameterizedTypes(EngineExecutionResults executionResults) {
		assertEquals(1, executionResults.testEvents().started().count(), "# tests started");
		assertEquals(1, executionResults.testEvents().succeeded().count(), "# tests succeeded");
		assertEquals(0, executionResults.testEvents().skipped().count(), "# tests skipped");
		assertEquals(0, executionResults.testEvents().aborted().count(), "# tests aborted");
		assertEquals(0, executionResults.testEvents().failed().count(), "# tests failed");
	}

	// -------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(CustomTypeParameterResolver.class)
	static class ConstructorInjectionTestCase {

		private final TestInfo outerTestInfo;
		private final CustomType outerCustomType;

		ConstructorInjectionTestCase(TestInfo testInfo, CustomType customType) {
			this.outerTestInfo = testInfo;
			this.outerCustomType = customType;
		}

		@Test
		void test() {
			assertNotNull(this.outerTestInfo);
			assertNotNull(this.outerCustomType);
		}

		@Nested
		class NestedTestCase {

			private final TestInfo innerTestInfo;
			private final CustomType innerCustomType;

			NestedTestCase(TestInfo testInfo, CustomType customType) {
				this.innerTestInfo = testInfo;
				this.innerCustomType = customType;
			}

			@Test
			void test() {
				assertNotNull(outerTestInfo);
				assertNotNull(outerCustomType);
				assertNotNull(this.innerTestInfo);
				assertNotNull(this.innerCustomType);
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(CustomAnnotationParameterResolver.class)
	static class AnnotatedParameterConstructorInjectionTestCase {

		private final TestInfo outerTestInfo;
		private final CustomType outerCustomType;

		AnnotatedParameterConstructorInjectionTestCase(TestInfo testInfo, @CustomAnnotation CustomType customType) {
			this.outerTestInfo = testInfo;
			this.outerCustomType = customType;
		}

		@Test
		void test() {
			assertNotNull(this.outerTestInfo);
			assertNotNull(this.outerCustomType);
		}

		@Nested
		// See https://github.com/junit-team/junit-framework/issues/1345
		class AnnotatedConstructorParameterNestedTestCase {

			private final TestInfo innerTestInfo;
			private final CustomType innerCustomType;

			AnnotatedConstructorParameterNestedTestCase(TestInfo testInfo, @CustomAnnotation CustomType customType) {
				this.innerTestInfo = testInfo;
				this.innerCustomType = customType;
			}

			@Test
			void test() {
				assertNotNull(outerTestInfo);
				assertNotNull(outerCustomType);
				assertNotNull(this.innerTestInfo);
				assertNotNull(this.innerCustomType);
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith({ CustomTypeParameterResolver.class, CustomAnnotationParameterResolver.class })
	static class MethodInjectionTestCase {

		@Test
		void parameterInjectionOfTestInfo(TestInfo testInfo) {
			assertNotNull(testInfo);
		}

		@Test
		void parameterInjectionWithCompetingResolversFail(@CustomAnnotation CustomType customType) {
			// should fail
		}

		@Test
		void parameterInjectionByType(CustomType customType) {
			assertNotNull(customType);
		}

		@Test
		void parameterInjectionByAnnotation(@CustomAnnotation String value) {
			assertNotNull(value);
		}

		// some overloaded methods

		@Test
		void overloadedName() {
			assertTrue(true);
		}

		@Test
		void overloadedName(CustomType customType) {
			assertNotNull(customType);
		}

		@Test
		void overloadedName(CustomType customType, @CustomAnnotation String value) {
			assertNotNull(customType);
			assertNotNull(value);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(NullIntegerParameterResolver.class)
	static class NullMethodInjectionTestCase {

		@Test
		void injectWrapper(Integer number) {
			assertNull(number);
		}

		@Test
		void injectPrimitive(int number) {
			// should never be invoked since an int cannot be null
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(PrimitiveIntegerParameterResolver.class)
	static class PrimitiveIntegerMethodInjectionTestCase {

		@Test
		void intPrimitive(int i) {
			assertEquals(42, i);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(PrimitiveArrayParameterResolver.class)
	static class PrimitiveArrayMethodInjectionTestCase {

		@Test
		void primitiveArray(int... ints) {
			assertArrayEquals(new int[] { 1, 2, 3 }, ints);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(NumberParameterResolver.class)
	static class PotentiallyIncompatibleTypeMethodInjectionTestCase {

		@Test
		void numberParameterInjection(Number number) {
			assertEquals(Integer.valueOf(42), number);
		}

		@Test
		void integerParameterInjection(Integer number) {
			assertEquals(Integer.valueOf(42), number);
		}

		/**
		 * This test must fail, since {@link Double} is a {@link Number} but not an {@link Integer}.
		 * @see NumberParameterResolver
		 */
		@Test
		void doubleParameterInjection(Double number) {
			/* no-op */
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class BeforeAndAfterMethodInjectionTestCase {

		@BeforeEach
		void before(TestInfo testInfo) {
			assertEquals("custom name", testInfo.getDisplayName());
		}

		@Test
		@DisplayName("custom name")
		void customNamedTest() {
		}

		@AfterEach
		void after(TestInfo testInfo) {
			assertEquals("custom name", testInfo.getDisplayName());
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@DisplayName("custom class name")
	static class BeforeAndAfterAllMethodInjectionTestCase {

		@BeforeAll
		static void beforeAll(TestInfo testInfo) {
			assertEquals("custom class name", testInfo.getDisplayName());
		}

		@Test
		void aTest() {
		}

		@AfterAll
		static void afterAll(TestInfo testInfo) {
			assertEquals("custom class name", testInfo.getDisplayName());
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ExtendWithOnMethodTestCase {

		/**
		 * This set-up / tear-down method is here to verify that {@code @BeforeEach}
		 * and {@code @AfterEach} methods are properly invoked using the same
		 * {@code ExtensionRegistry} as the one used for the corresponding
		 * {@code @Test} method.
		 *
		 * @see <a href="https://github.com/junit-team/junit-framework/issues/523">#523</a>
		 */
		@SuppressWarnings("JUnitMalformedDeclaration")
		@BeforeEach
		@AfterEach
		void setUpAndTearDown(CustomType customType, @CustomAnnotation String value) {
			assertNotNull(customType);
			assertNotNull(value);
		}

		@Test
		@ExtendWith(CustomTypeParameterResolver.class)
		@ExtendWith(CustomAnnotationParameterResolver.class)
		void testMethodWithExtensionAnnotation(CustomType customType, @CustomAnnotation String value) {
			assertNotNull(customType);
			assertNotNull(value);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ParameterizedTypeTestCase {

		@Test
		@ExtendWith(MapOfStringsParameterResolver.class)
		void testMapOfStrings(Map<String, String> map) {
			assertNotNull(map);
			assertEquals("value", map.get("key"));
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TypeBasedParameterResolverTestCase {
		@Test
		@ExtendWith(MapOfListsTypeBasedParameterResolver.class)
		void testMapOfLists(Map<String, List<Integer>> map) {
			assertNotNull(map);
			assertEquals(asList(1, 42), map.get("ids"));
		}
	}

}
