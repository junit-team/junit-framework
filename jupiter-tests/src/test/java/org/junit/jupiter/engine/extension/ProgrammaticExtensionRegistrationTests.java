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

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields;
import static org.junit.platform.commons.support.ReflectionSupport.makeAccessible;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.cause;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.assertj.core.api.Condition;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.execution.injection.sample.LongParameterResolver;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests that verify support for programmatic extension registration
 * via {@link RegisterExtension @RegisterExtension} in the {@link JupiterTestEngine}.
 *
 * @since 5.1
 * @see OrderedProgrammaticExtensionRegistrationTests
 */
class ProgrammaticExtensionRegistrationTests extends AbstractJupiterTestEngineTests {

	private static final List<String> callSequence = new ArrayList<>();

	@BeforeEach
	void clearCallSequence() {
		callSequence.clear();
	}

	@Test
	void instanceLevel() {
		assertOneTestSucceeded(InstanceLevelExtensionRegistrationTestCase.class);
	}

	@Test
	void instanceLevelWithInjectedExtension() {
		assertOneTestSucceeded(InstanceLevelExtensionRegistrationWithInjectedExtensionTestCase.class);
	}

	@Test
	void instanceLevelWithTestInstancePerClassLifecycle() {
		assertOneTestSucceeded(InstanceLevelExtensionRegistrationWithTestInstancePerClassLifecycleTestCase.class);
	}

	@Test
	void classLevel() {
		assertOneTestSucceeded(ClassLevelExtensionRegistrationTestCase.class);
	}

	@Test
	void classLevelFromSuperclass() {
		assertOneTestSucceeded(SubClassLevelExtensionRegistrationTestCase.class);
	}

	@Test
	void classLevelFromInterface() {
		assertOneTestSucceeded(ExtensionRegistrationFromInterfaceTestCase.class);
	}

	@Test
	void instanceLevelWithInheritedExtensions() {
		Class<?> testClass = InstanceLevelExtensionRegistrationParentTestCase.class;
		String parent = testClass.getSimpleName();
		assertOneTestSucceeded(testClass);
		assertThat(callSequence).containsExactly( //
			parent + " :: extension1 :: before test", //
			parent + " :: extension2 :: before test" //
		);

		callSequence.clear();
		testClass = InstanceLevelExtensionRegistrationChildTestCase.class;
		String child = testClass.getSimpleName();
		assertOneTestSucceeded(testClass);
		assertThat(callSequence).containsExactly( //
			parent + " :: extension1 :: before test", //
			parent + " :: extension2 :: before test", //
			child + " :: extension2 :: before test", //
			child + " :: extension3 :: before test" //
		);
	}

	@Test
	void classLevelWithInheritedExtensions() {
		Class<?> testClass = ClassLevelExtensionRegistrationParentTestCase.class;
		String parent = testClass.getSimpleName();
		assertOneTestSucceeded(testClass);
		assertThat(callSequence).containsExactly( //
			parent + " :: extension1 :: before test", //
			parent + " :: extension2 :: before test" //
		);

		callSequence.clear();
		testClass = ClassLevelExtensionRegistrationChildTestCase.class;
		String child = testClass.getSimpleName();
		assertOneTestSucceeded(testClass);
		assertThat(callSequence).containsExactly( //
			parent + " :: extension1 :: before test", //
			parent + " :: extension2 :: before test", //
			child + " :: extension2 :: before test", //
			child + " :: extension3 :: before test" //
		);
	}

	/**
	 * @since 5.5
	 */
	@Test
	void instanceLevelWithFieldThatDoesNotImplementAnExtensionApi() {
		assertOneTestSucceeded(InstanceLevelCustomExtensionApiTestCase.class);
		assertThat(callSequence).containsExactly( //
			CustomExtensionImpl.class.getSimpleName() + " :: before test", //
			CustomExtensionImpl.class.getSimpleName() + " :: doSomething()" //
		);
	}

	/**
	 * @since 5.5
	 */
	@Test
	void classLevelWithFieldThatDoesNotImplementAnExtensionApi() {
		assertOneTestSucceeded(ClassLevelCustomExtensionApiTestCase.class);
		assertThat(callSequence).containsExactly( //
			CustomExtensionImpl.class.getSimpleName() + " :: before test", //
			CustomExtensionImpl.class.getSimpleName() + " :: doSomething()" //
		);
	}

	/**
	 * @since 5.5
	 */
	@Test
	void instanceLevelWithPrivateField() {
		Class<?> testClass = InstanceLevelExtensionRegistrationWithPrivateFieldTestCase.class;
		executeTestsForClass(testClass).testEvents().assertStatistics(stats -> stats.succeeded(1));
	}

	/**
	 * @since 5.5
	 */
	@Test
	void classLevelWithPrivateField() {
		Class<?> testClass = ClassLevelExtensionRegistrationWithPrivateFieldTestCase.class;
		executeTestsForClass(testClass).testEvents().assertStatistics(stats -> stats.succeeded(1));
	}

	@Test
	void instanceLevelWithNullField() {
		Class<?> testClass = InstanceLevelExtensionRegistrationWithNullFieldTestCase.class;

		executeTestsForClass(testClass).testEvents().debug().assertThatEvents().haveExactly(1, finishedWithFailure(
			instanceOf(PreconditionViolationException.class), message(expectedMessage(testClass, null))));
	}

	@Test
	void classLevelWithNullField() {
		Class<?> testClass = ClassLevelExtensionRegistrationWithNullFieldTestCase.class;

		executeTestsForClass(testClass).containerEvents().assertThatEvents().haveExactly(1, finishedWithFailure(
			instanceOf(PreconditionViolationException.class), message(expectedMessage(testClass, null))));
	}

	/**
	 * @since 5.5
	 */
	@Test
	void instanceLevelWithNonExtensionFieldValue() {
		Class<?> testClass = InstanceLevelExtensionRegistrationWithNonExtensionFieldValueTestCase.class;

		executeTestsForClass(testClass).testEvents().assertThatEvents().haveExactly(1, finishedWithFailure(
			instanceOf(PreconditionViolationException.class), message(expectedMessage(testClass, String.class))));
	}

	/**
	 * @since 5.5
	 */
	@Test
	void classLevelWithNonExtensionFieldValue() {
		Class<?> testClass = ClassLevelExtensionRegistrationWithNonExtensionFieldValueTestCase.class;

		executeTestsForClass(testClass).containerEvents().assertThatEvents().haveExactly(1, finishedWithFailure(
			instanceOf(PreconditionViolationException.class), message(expectedMessage(testClass, String.class))));
	}

	private String expectedMessage(Class<?> testClass, @Nullable Class<?> valueType) {
		return "Failed to register extension via @RegisterExtension field [" + field(testClass)
				+ "]: field value's type [" + (valueType != null ? valueType.getName() : null) + "] must implement an ["
				+ Extension.class.getName() + "] API.";
	}

	private Field field(Class<?> testClass) {
		try {
			return testClass.getDeclaredField("extension");
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Test
	void propagatesCheckedExceptionThrownDuringInitializationOfStaticField() {
		assertClassFails(ClassLevelExplosiveCheckedExceptionTestCase.class,
			allOf(instanceOf(ExceptionInInitializerError.class), cause(instanceOf(Exception.class), message("boom"))));
	}

	@Test
	void propagatesUncheckedExceptionThrownDuringInitializationOfStaticField() {
		assertClassFails(ClassLevelExplosiveUncheckedExceptionTestCase.class, allOf(
			instanceOf(ExceptionInInitializerError.class), cause(instanceOf(RuntimeException.class), message("boom"))));
	}

	@Test
	void propagatesErrorThrownDuringInitializationOfStaticField() {
		assertClassFails(ClassLevelExplosiveErrorTestCase.class, allOf(instanceOf(Error.class), message("boom")));
	}

	@Test
	void propagatesCheckedExceptionThrownDuringInitializationOfInstanceField() {
		assertTestFails(InstanceLevelExplosiveCheckedExceptionTestCase.class,
			allOf(instanceOf(Exception.class), message("boom")));
	}

	@Test
	void propagatesUncheckedExceptionThrownDuringInitializationOfInstanceField() {
		assertTestFails(InstanceLevelExplosiveUncheckedExceptionTestCase.class,
			allOf(instanceOf(RuntimeException.class), message("boom")));
	}

	@Test
	void propagatesErrorThrownDuringInitializationOfInstanceField() {
		assertTestFails(InstanceLevelExplosiveErrorTestCase.class, allOf(instanceOf(Error.class), message("boom")));
	}

	@Test
	void storesExtensionInRegistryOfNestedTestMethods() {
		var results = executeTestsForClass(TwoNestedClassesTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.succeeded(4));
	}

	private void assertClassFails(Class<?> testClass, Condition<Throwable> causeCondition) {
		EngineExecutionResults executionResults = executeTestsForClass(testClass);
		executionResults.containerEvents().assertThatEvents().haveExactly(1, finishedWithFailure(causeCondition));
	}

	private void assertTestFails(Class<?> testClass, Condition<Throwable> causeCondition) {
		executeTestsForClass(testClass).testEvents().assertThatEvents().haveExactly(1,
			finishedWithFailure(causeCondition));
	}

	private void assertOneTestSucceeded(Class<?> testClass) {
		executeTestsForClass(testClass).testEvents().assertStatistics(
			stats -> stats.started(1).succeeded(1).skipped(0).aborted(0).failed(0));
	}

	// -------------------------------------------------------------------

	private static void assertWisdom(CrystalBall crystalBall, String wisdom, String useCase) {
		assertNotNull(crystalBall, useCase);
		assertEquals("Outlook good", wisdom, useCase);
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InstanceLevelExtensionRegistrationTestCase {

		@RegisterExtension
		final CrystalBall crystalBall = new CrystalBall("Outlook good");

		@BeforeEach
		void beforeEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@BeforeEach");
		}

		@Test
		void test(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@Test");
		}

		@AfterEach
		void afterEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@AfterEach");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(ExtensionInjector.class)
	@NullUnmarked
	static class InstanceLevelExtensionRegistrationWithInjectedExtensionTestCase {

		@RegisterExtension
		protected CrystalBall crystalBall; // Injected by ExtensionInjector.

		@BeforeEach
		void beforeEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@BeforeEach");
		}

		@Test
		void test(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@Test");
		}

		@AfterEach
		void afterEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@AfterEach");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestInstance(PER_CLASS)
	static class InstanceLevelExtensionRegistrationWithTestInstancePerClassLifecycleTestCase {

		@RegisterExtension
		final CrystalBall crystalBall = new CrystalBall("Outlook good");

		@BeforeAll
		void beforeAll(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@BeforeAll");
		}

		@BeforeEach
		void beforeEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@BeforeEach");
		}

		@Test
		void test(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@Test");
		}

		@AfterEach
		void afterEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@AfterEach");
		}

		@AfterAll
		void afterAll(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@AfterAll");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ClassLevelExtensionRegistrationTestCase {

		@RegisterExtension
		static final CrystalBall crystalBall = new CrystalBall("Outlook good");

		@BeforeAll
		static void beforeAll(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@BeforeAll");
		}

		@BeforeEach
		void beforeEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@BeforeEach");
		}

		@Test
		void test(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@Test");
		}

		@AfterEach
		void afterEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@AfterEach");
		}

		@AfterAll
		static void afterAll(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@AfterAll");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class SubClassLevelExtensionRegistrationTestCase extends ClassLevelExtensionRegistrationTestCase {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@Test
		@Override
		void test(String wisdom) {
			assertWisdom(crystalBall, wisdom, "Overridden @Test");
		}

	}

	interface ClassLevelExtensionRegistrationInterface {

		@RegisterExtension
		CrystalBall crystalBall = new CrystalBall("Outlook good");

		@BeforeAll
		static void beforeAll(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@BeforeAll");
		}

		@BeforeEach
		default void beforeEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@BeforeEach");
		}

		@AfterEach
		default void afterEach(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@AfterEach");
		}

		@AfterAll
		static void afterAll(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@AfterAll");
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ExtensionRegistrationFromInterfaceTestCase implements ClassLevelExtensionRegistrationInterface {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@Test
		void test(String wisdom) {
			assertWisdom(crystalBall, wisdom, "@Test");
		}

	}

	private record CrystalBall(String wisdom) implements ParameterResolver {

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return parameterContext.getParameter().getType() == String.class;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return this.wisdom;
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ClassLevelExtensionRegistrationParentTestCase {

		@RegisterExtension
		static Extension extension1 = new BeforeEachExtension(1);

		@RegisterExtension
		static Extension extension2 = new BeforeEachExtension(2);

		@Test
		void test() {
		}

	}

	static class ClassLevelExtensionRegistrationChildTestCase extends ClassLevelExtensionRegistrationParentTestCase {

		// "Hides" ClassLevelExtensionRegistrationParentTestCase.extension2 in legacy mode
		@RegisterExtension
		static Extension extension2 = new BeforeEachExtension(2);

		@RegisterExtension
		static Extension extension3 = new BeforeEachExtension(3);

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InstanceLevelExtensionRegistrationParentTestCase {

		@RegisterExtension
		Extension extension1 = new BeforeEachExtension(1);

		@RegisterExtension
		Extension extension2 = new BeforeEachExtension(2);

		@Test
		void test() {
		}

	}

	static class InstanceLevelExtensionRegistrationChildTestCase
			extends InstanceLevelExtensionRegistrationParentTestCase {

		// "Hides" InstanceLevelExtensionRegistrationParentTestCase.extension2 in legacy mode
		@RegisterExtension
		Extension extension2 = new BeforeEachExtension(2);

		@RegisterExtension
		Extension extension3 = new BeforeEachExtension(3);

	}

	private static class BeforeEachExtension implements BeforeEachCallback {

		private final String prefix;

		BeforeEachExtension(int id) {
			Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
			this.prefix = callerClass.getSimpleName() + " :: extension" + id + " :: before ";
		}

		@Override
		public void beforeEach(ExtensionContext context) {
			callSequence.add(this.prefix + context.getRequiredTestMethod().getName());
		}

	}

	/**
	 * This interface intentionally does not implement a supported {@link Extension} API.
	 */
	interface CustomExtension {

		void doSomething();

	}

	static class CustomExtensionImpl implements CustomExtension, BeforeEachCallback {

		@Override
		public void doSomething() {
			callSequence.add(getClass().getSimpleName() + " :: doSomething()");
		}

		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			callSequence.add(getClass().getSimpleName() + " :: before " + context.getRequiredTestMethod().getName());
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InstanceLevelCustomExtensionApiTestCase {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@RegisterExtension
		CustomExtension extension = new CustomExtensionImpl();

		@Test
		void test() {
			this.extension.doSomething();
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ClassLevelCustomExtensionApiTestCase {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@RegisterExtension
		static CustomExtension extension = new CustomExtensionImpl();

		@Test
		void test() {
			extension.doSomething();
		}

	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class AbstractTestCase {

		@Test
		void test() {
		}

	}

	static class InstanceLevelExtensionRegistrationWithPrivateFieldTestCase extends AbstractTestCase {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@RegisterExtension
		private Extension extension = new Extension() {
		};

	}

	static class ClassLevelExtensionRegistrationWithPrivateFieldTestCase extends AbstractTestCase {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@RegisterExtension
		private static Extension extension = new Extension() {
		};

	}

	@NullUnmarked
	static class InstanceLevelExtensionRegistrationWithNullFieldTestCase extends AbstractTestCase {

		@RegisterExtension
		Extension extension;

	}

	@NullUnmarked
	static class ClassLevelExtensionRegistrationWithNullFieldTestCase extends AbstractTestCase {

		@RegisterExtension
		static Extension extension;

	}

	static class InstanceLevelExtensionRegistrationWithNonExtensionFieldValueTestCase extends AbstractTestCase {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@RegisterExtension
		Object extension = "not an extension type";

	}

	static class ClassLevelExtensionRegistrationWithNonExtensionFieldValueTestCase extends AbstractTestCase {

		@SuppressWarnings("JUnitMalformedDeclaration")
		@RegisterExtension
		static Object extension = "not an extension type";

	}

	static class ClassLevelExplosiveCheckedExceptionTestCase extends AbstractTestCase {

		@RegisterExtension
		static Extension field = new ExplosiveExtension(new Exception("boom"));

	}

	static class ClassLevelExplosiveUncheckedExceptionTestCase extends AbstractTestCase {

		@RegisterExtension
		static Extension field = new ExplosiveExtension(new RuntimeException("boom"));

	}

	static class ClassLevelExplosiveErrorTestCase extends AbstractTestCase {

		@RegisterExtension
		static Extension field = new ExplosiveExtension(new Error("boom"));

	}

	static class InstanceLevelExplosiveCheckedExceptionTestCase extends AbstractTestCase {

		@RegisterExtension
		Extension field = new ExplosiveExtension(new Exception("boom"));

	}

	static class InstanceLevelExplosiveUncheckedExceptionTestCase extends AbstractTestCase {

		@RegisterExtension
		Extension field = new ExplosiveExtension(new RuntimeException("boom"));

	}

	static class InstanceLevelExplosiveErrorTestCase extends AbstractTestCase {

		@RegisterExtension
		Extension field = new ExplosiveExtension(new Error("boom"));

	}

	static class ExplosiveExtension implements Extension {

		ExplosiveExtension(Throwable t) {
			throw ExceptionUtils.throwAsUncheckedException(t);
		}

	}

	/**
	 * Mimics a dependency injection framework such as Spring, Guice, CDI, etc.,
	 * where the instance of the extension registered via
	 * {@link RegisterExtension @RegisterExtension} is managed by the DI
	 * framework and injected into the test instance.
	 */
	private static class ExtensionInjector implements TestInstancePostProcessor {

		private static final Predicate<Field> isCrystalBall = field -> CrystalBall.class.isAssignableFrom(
			field.getType());

		@Override
		public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
			// @formatter:off
			findAnnotatedFields(testInstance.getClass(), RegisterExtension.class, isCrystalBall).stream()
				.findFirst()
				.ifPresent(field -> {
					try {
						makeAccessible(field).set(testInstance, new CrystalBall("Outlook good"));
					}
					catch (Throwable t) {
						throw ExceptionUtils.throwAsUncheckedException(t);
					}
				});
			// @formatter:on
		}

	}

	static class TwoNestedClassesTestCase {

		@RegisterExtension
		Extension extension = new LongParameterResolver();

		@Nested
		class A {

			@SuppressWarnings("JUnitMalformedDeclaration")
			@Test
			void first(Long n) {
				assertEquals(42L, n);
			}

			@SuppressWarnings("JUnitMalformedDeclaration")
			@Test
			void second(Long n) {
				assertEquals(42L, n);
			}

		}

		@Nested
		class B {

			@SuppressWarnings("JUnitMalformedDeclaration")
			@Test
			void first(Long n) {
				assertEquals(42L, n);
			}

			@SuppressWarnings("JUnitMalformedDeclaration")
			@Test
			void second(Long n) {
				assertEquals(42L, n);
			}

		}

	}

}
