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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.Events;
import org.junit.platform.testkit.engine.Execution;

/**
 * Integration tests for {@link AutoClose @AutoClose} and the {@link AutoCloseExtension}.
 *
 * @since 5.11
 */
class AutoCloseTests extends AbstractJupiterTestEngineTests {

	private static final List<String> recorder = new ArrayList<>();

	@BeforeEach
	@AfterEach
	void resetTracking() {
		InstancePerClassTestCase.closed = false;
		recorder.clear();
	}

	@Test
	void blankCloseMethodName() {
		Class<?> testClass = BlankCloseMethodNameTestCase.class;
		String msg = "@AutoClose on field %s.field must specify a method name.".formatted(testClass.getCanonicalName());
		Events tests = executeTestsForClass(testClass).testEvents();
		assertFailingWithMessage(tests, msg);
	}

	@Test
	void primitiveTypeCannotBeClosed() {
		Class<?> testClass = PrimitiveFieldTestCase.class;
		String msg = "@AutoClose is not supported on primitive field %s.x.".formatted(testClass.getCanonicalName());
		Events tests = executeTestsForClass(testClass).testEvents();
		assertFailingWithMessage(tests, msg);
	}

	@Test
	void arrayCannotBeClosed() {
		Class<?> testClass = ArrayFieldTestCase.class;
		String msg = "@AutoClose is not supported on array field %s.x.".formatted(testClass.getCanonicalName());
		Events tests = executeTestsForClass(testClass).testEvents();
		assertFailingWithMessage(tests, msg);
	}

	@Test
	void nullCannotBeClosed(@TrackLogRecords LogRecordListener listener) {
		Class<?> testClass = NullCloseableFieldTestCase.class;
		String msg = "Cannot @AutoClose field %s.field because it is null.".formatted(testClass.getCanonicalName());
		Events tests = executeTestsForClass(testClass).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(1).failed(0));
		assertThat(listener.stream(Level.WARNING)).map(LogRecord::getMessage).anyMatch(msg::equals);
	}

	@Test
	void noCloseMethod() {
		assertMissingCloseMethod(NoCloseMethodTestCase.class, "close");
	}

	@Test
	void noShutdownMethod() {
		assertMissingCloseMethod(NoShutdownMethodTestCase.class, "shutdown");
	}

	/**
	 * Tests prerequisites for the {@link AutoCloseSpy} implementation.
	 */
	@Test
	void spyPermitsOnlyASingleAction() {
		@SuppressWarnings("resource")
		AutoCloseSpy spy = new AutoCloseSpy("preconditions");

		spy.close();

		assertThatIllegalStateException().isThrownBy(spy::run).withMessage("Already closed via close()");
		assertThatIllegalStateException().isThrownBy(spy::close).withMessage("Already closed via close()");
		assertThat(recorder).containsExactly("AutoCloseTests.preconditions.close()");
	}

	/**
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/3684">#3684</a>
	 */
	@Test
	void fieldsAreProperlyClosedViaInterfaceMethods() {
		// If the test method succeeds, that means there was no issue invoking
		// the @AutoClose fields. No need to assert anything else for this use case.
		executeTestsForClass(CloseMethodMustBeInvokedViaInterfaceTestCase.class)//
				.testEvents()//
				.assertStatistics(stats -> stats.succeeded(1));
	}

	@Test
	void fieldsAreProperlyClosedWithInstancePerMethodTestClass() {
		Events tests = executeTestsForClass(InstancePerMethodTestCase.class).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(2));
		assertThat(recorder).containsExactly(//
			// test1()
			"InstancePerMethodTestCase.runnable.run()", //
			"InstancePerMethodTestCase.closable.close()", //
			// test2()
			"InstancePerMethodTestCase.runnable.run()", //
			"InstancePerMethodTestCase.closable.close()", //
			// Class-level cleanup
			"InstancePerMethodTestCase.staticClosable.close()"//
		);
	}

	@Test
	void fieldsAreProperlyClosedWithInstancePerClassTestClass() {
		Events tests = executeTestsForClass(InstancePerClassTestCase.class).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(2));
		assertThat(InstancePerClassTestCase.closed).isTrue();
	}

	@Test
	void fieldsAreProperlyClosedWithNestedTestClassesWithInstancePerMethod() {
		Events tests = executeTestsForClass(InstancePerMethodEnclosingTestCase.NestedTestCase.class).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(1));
		assertThat(recorder).containsExactly(//
			"NestedTestCase.nestedClosable.close()", //
			"InstancePerMethodEnclosingTestCase.enclosingClosable.close()", //
			"NestedTestCase.nestedStaticClosable.close()", //
			"InstancePerMethodEnclosingTestCase.enclosingStaticClosable.close()"//
		);

		// Reset tracking
		resetTracking();

		tests = executeTestsForClass(InstancePerMethodEnclosingTestCase.class).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(2));
		assertThat(recorder).containsExactly(//
			"InstancePerMethodEnclosingTestCase.enclosingClosable.close()", //
			"NestedTestCase.nestedClosable.close()", //
			"InstancePerMethodEnclosingTestCase.enclosingClosable.close()", //
			"NestedTestCase.nestedStaticClosable.close()", //
			"InstancePerMethodEnclosingTestCase.enclosingStaticClosable.close()" //
		);
	}

	@Test
	void fieldsAreProperlyClosedWithNestedTestClassesWithInstancePerClass() {
		// With test instance lifecycle "per class" mode, we actually expect the
		// same behavior for the closing of all fields when the nested test class
		// is run standalone AND when it's run along with its enclosing class.
		String[] expected = { //
				"NestedTestCase.nestedStaticClosable.close()", //
				"NestedTestCase.nestedClosable.close()", //
				"InstancePerClassEnclosingTestCase.enclosingStaticClosable.close()", //
				"InstancePerClassEnclosingTestCase.enclosingClosable.close()" //
		};

		Events tests = executeTestsForClass(InstancePerClassEnclosingTestCase.NestedTestCase.class).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(1));
		assertThat(recorder).containsExactly(expected);

		// Reset tracking
		resetTracking();

		tests = executeTestsForClass(InstancePerClassEnclosingTestCase.class).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(2));
		assertThat(recorder).containsExactly(expected);
	}

	@Test
	void fieldsAreProperlyClosedWithinTestClassHierarchy() {
		Events tests = executeTestsForClass(SuperTestCase.class).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(1));
		assertThat(recorder).containsExactly(//
			// superTest()
			"SuperTestCase.superClosable.close()", //
			// Class-level cleanup
			"SuperTestCase.superStaticClosable.close()" //
		);

		// Reset tracking
		resetTracking();

		tests = executeTestsForClass(SubTestCase.class).testEvents();
		tests.assertStatistics(stats -> stats.succeeded(2));
		assertThat(recorder).containsExactly(//
			// superTest()
			"SubTestCase.subClosable.close()", //
			"SuperTestCase.superClosable.close()", //
			// subTest()
			"SubTestCase.subClosable.close()", //
			"SuperTestCase.superClosable.close()", //
			// Class-level cleanup in subclass
			"SubTestCase.subStaticClosable.close()", //
			// Class-level cleanup in superclass
			"SuperTestCase.superStaticClosable.close()" //
		);
	}

	@Test
	void allFieldsAreClosedIfAnyFieldThrowsAnException() {
		// Prerequisites to ensure fields are "ordered" as expected (based on the hash codes for their names).
		assertThat("staticField1".hashCode()).isLessThan("staticField2".hashCode()).isLessThan(
			"staticField3".hashCode());
		assertThat("field1".hashCode()).isLessThan("field2".hashCode()).isLessThan("field3".hashCode());

		Class<?> testClass = FailingFieldsTestCase.class;
		EngineExecutionResults allEvents = executeTestsForClass(testClass);

		Events tests = allEvents.testEvents();
		tests.assertStatistics(stats -> stats.succeeded(0).failed(1));

		// Verify that ALL fields were closed in the proper order.
		assertThat(recorder).containsExactly(//
			"FailingFieldsTestCase.field1.close()", //
			"FailingFieldsTestCase.field2.close()", //
			"FailingFieldsTestCase.field3.close()", //
			"FailingFieldsTestCase.staticField1.close()", //
			"FailingFieldsTestCase.staticField2.close()", //
			"FailingFieldsTestCase.staticField3.close()" //
		);

		// Test-level failures
		assertThat(findFailure(tests, "test()")) //
				.isExactlyInstanceOf(RuntimeException.class) //
				.hasMessage("FailingFieldsTestCase.field1.close()")//
				.hasNoCause()//
				.hasSuppressedException(new RuntimeException("FailingFieldsTestCase.field2.close()"));

		Events containers = allEvents.containerEvents();
		containers.assertStatistics(stats -> stats.succeeded(1).failed(1));

		// Container-level failures
		assertThat(findFailure(containers, testClass.getSimpleName())) //
				.isExactlyInstanceOf(RuntimeException.class) //
				.hasMessage("FailingFieldsTestCase.staticField1.close()")//
				.hasNoCause()//
				.hasSuppressedException(new RuntimeException("FailingFieldsTestCase.staticField2.close()"));
	}

	@Test
	void allFieldsAreClosedIfAnyFieldThrowsAnExceptionWithNestedTestClassesWithInstancePerMethod() {
		Class<?> enclosingTestClass = FailingFieldsEnclosingTestCase.class;
		Class<?> nestedTestClass = FailingFieldsEnclosingTestCase.NestedTestCase.class;

		EngineExecutionResults allEvents = executeTestsForClass(nestedTestClass);
		Events tests = allEvents.testEvents();
		tests.assertStatistics(stats -> stats.succeeded(0).failed(1));

		// Verify that ALL fields were closed in the proper order.
		assertThat(recorder).containsExactly(//
			// Results from NestedTestCase instance
			"NestedTestCase.nestedField1.close()", //
			"NestedTestCase.nestedField2.close()", //
			// Results from FailingFieldsEnclosingTestCase instance
			"FailingFieldsEnclosingTestCase.enclosingField1.close()", //
			"FailingFieldsEnclosingTestCase.enclosingField2.close()", //
			// Results from NestedTestCase class
			"NestedTestCase.nestedStaticField1.close()", //
			"NestedTestCase.nestedStaticField2.close()", //
			// Results from FailingFieldsEnclosingTestCase class
			"FailingFieldsEnclosingTestCase.enclosingStaticField1.close()", //
			"FailingFieldsEnclosingTestCase.enclosingStaticField2.close()"//
		);

		// Test-level failures
		assertThat(findFailure(tests, "nestedTest()"))//
				.isExactlyInstanceOf(RuntimeException.class)//
				.hasMessage("NestedTestCase.nestedField1.close()")//
				.hasNoCause()//
				.hasSuppressedException(new RuntimeException("FailingFieldsEnclosingTestCase.enclosingField1.close()"));

		Events containers = allEvents.containerEvents();
		containers.assertStatistics(stats -> stats.succeeded(1).failed(2));

		// Container-level failures
		assertThat(findFailure(containers, nestedTestClass.getSimpleName()))//
				.isExactlyInstanceOf(RuntimeException.class)//
				.hasMessage("NestedTestCase.nestedStaticField1.close()")//
				.hasNoCause()//
				.hasNoSuppressedExceptions();
		assertThat(findFailure(containers, enclosingTestClass.getSimpleName()))//
				.isExactlyInstanceOf(RuntimeException.class)//
				.hasMessage("FailingFieldsEnclosingTestCase.enclosingStaticField1.close()")//
				.hasNoCause()//
				.hasNoSuppressedExceptions();

		// Reset tracking
		resetTracking();

		allEvents = executeTestsForClass(enclosingTestClass);
		tests = allEvents.testEvents();
		tests.assertStatistics(stats -> stats.succeeded(0).failed(2));

		// Verify that ALL fields were closed in the proper order.
		assertThat(recorder).containsExactly(//
			// Results from FailingFieldsEnclosingTestCase instance
			"FailingFieldsEnclosingTestCase.enclosingField1.close()", //
			"FailingFieldsEnclosingTestCase.enclosingField2.close()", //

			// Results from NestedTestCase instance
			"NestedTestCase.nestedField1.close()", //
			"NestedTestCase.nestedField2.close()", //
			// Results from FailingFieldsEnclosingTestCase instance
			"FailingFieldsEnclosingTestCase.enclosingField1.close()", //
			"FailingFieldsEnclosingTestCase.enclosingField2.close()", //
			// Results from NestedTestCase class
			"NestedTestCase.nestedStaticField1.close()", //
			"NestedTestCase.nestedStaticField2.close()", //
			// Results from FailingFieldsEnclosingTestCase class
			"FailingFieldsEnclosingTestCase.enclosingStaticField1.close()", //
			"FailingFieldsEnclosingTestCase.enclosingStaticField2.close()"//
		);

		// Test-level failures
		assertThat(findFailure(tests, "enclosingTest()"))//
				.isExactlyInstanceOf(RuntimeException.class)//
				.hasMessage("FailingFieldsEnclosingTestCase.enclosingField1.close()")//
				.hasNoCause()//
				.hasNoSuppressedExceptions();
		assertThat(findFailure(tests, "nestedTest()"))//
				.isExactlyInstanceOf(RuntimeException.class)//
				.hasMessage("NestedTestCase.nestedField1.close()")//
				.hasNoCause()//
				.hasSuppressedException(new RuntimeException("FailingFieldsEnclosingTestCase.enclosingField1.close()"));

		containers = allEvents.containerEvents();
		containers.assertStatistics(stats -> stats.succeeded(1).failed(2));

		// Container-level failures
		assertThat(findFailure(containers, nestedTestClass.getSimpleName()))//
				.isExactlyInstanceOf(RuntimeException.class)//
				.hasMessage("NestedTestCase.nestedStaticField1.close()")//
				.hasNoCause()//
				.hasNoSuppressedExceptions();
		assertThat(findFailure(containers, enclosingTestClass.getSimpleName()))//
				.isExactlyInstanceOf(RuntimeException.class)//
				.hasMessage("FailingFieldsEnclosingTestCase.enclosingStaticField1.close()")//
				.hasNoCause()//
				.hasNoSuppressedExceptions();
	}

	private Throwable findFailure(Events tests, String displayName) {
		return findExecution(tests, displayName)//
				.getTerminationInfo().getExecutionResult().getThrowable().orElseThrow();
	}

	private static Execution findExecution(Events events, String displayName) {
		return events.executions()//
				.filter(execution -> execution.getTestDescriptor().getDisplayName().contains(displayName))//
				.findFirst().get();
	}

	private static void assertFailingWithMessage(Events testEvents, String msg) {
		testEvents//
				.assertStatistics(stats -> stats.failed(1))//
				.assertThatEvents().haveExactly(1, finishedWithFailure(message(msg)));
	}

	private void assertMissingCloseMethod(Class<?> testClass, String methodName) {
		String msg = "Cannot @AutoClose field %s.field because %s does not define method %s().".formatted(
			testClass.getCanonicalName(), String.class.getName(), methodName);
		Events tests = executeTestsForClass(testClass).testEvents();
		assertFailingWithMessage(tests, msg);
	}

	interface TestInterface {

		@Test
		default void test() {
		}
	}

	static class BlankCloseMethodNameTestCase implements TestInterface {

		@AutoClose("")
		final String field = "blank";
	}

	static class PrimitiveFieldTestCase implements TestInterface {

		@AutoClose
		final int x = 0;
	}

	static class ArrayFieldTestCase implements TestInterface {

		@AutoClose
		final int[] x = {};
	}

	@NullUnmarked
	static class NullCloseableFieldTestCase implements TestInterface {

		@AutoClose
		final AutoCloseable field = null;
	}

	static class NoCloseMethodTestCase implements TestInterface {

		@AutoClose
		private final String field = "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@AutoClose("shutdown")
	@interface AutoShutdown {
	}

	static class NoShutdownMethodTestCase implements TestInterface {

		@AutoShutdown
		private final String field = "";
	}

	static class CloseMethodMustBeInvokedViaInterfaceTestCase implements TestInterface {

		@AutoClose
		final InputStream inputStream = InputStream.nullInputStream();

		@AutoClose("shutdown")
		final ExecutorService service = Executors.newSingleThreadExecutor();
	}

	@TestInstance(PER_METHOD)
	@SuppressWarnings("JUnitMalformedDeclaration")
	@NullUnmarked
	static class InstancePerMethodTestCase {

		@AutoClose
		private static AutoCloseable staticClosable;

		@AutoClose
		private static final AutoCloseable nullStatic = null;

		@AutoClose
		private final AutoCloseable closable = new AutoCloseSpy("closable");

		@AutoClose("   run      ") // intentionally contains extra whitespace.
		private final Runnable runnable = new AutoCloseSpy("runnable");

		@AutoClose
		private final AutoCloseable nullField = null;

		@BeforeAll
		static void setup() {
			staticClosable = new AutoCloseSpy("staticClosable");
		}

		@Test
		void test1() {
		}

		@Test
		void test2() {
		}
	}

	@TestInstance(PER_CLASS)
	@SuppressWarnings("JUnitMalformedDeclaration")
	static class InstancePerClassTestCase {

		static boolean closed = false;

		@AutoClose
		final AutoCloseable field = () -> closed = true;

		@Test
		void test1() {
			assertThat(closed).isFalse();
		}

		@Test
		void test2() {
			assertThat(closed).isFalse();
		}
	}

	@TestInstance(PER_METHOD)
	static class InstancePerMethodEnclosingTestCase implements TestInterface {

		@AutoClose
		static AutoCloseSpy enclosingStaticClosable;

		@AutoClose
		final AutoCloseable enclosingClosable = new AutoCloseSpy("enclosingClosable");

		@BeforeAll
		static void setup() {
			enclosingStaticClosable = new AutoCloseSpy("enclosingStaticClosable");
		}

		@Nested
		@TestInstance(PER_METHOD)
		class NestedTestCase implements TestInterface {

			@AutoClose
			static AutoCloseSpy nestedStaticClosable;

			@AutoClose
			final AutoCloseable nestedClosable = new AutoCloseSpy("nestedClosable");

			@BeforeAll
			static void setup() {
				nestedStaticClosable = new AutoCloseSpy("nestedStaticClosable");
			}
		}
	}

	@TestInstance(PER_CLASS)
	static class InstancePerClassEnclosingTestCase implements TestInterface {

		@AutoClose
		static AutoCloseSpy enclosingStaticClosable;

		@AutoClose
		final AutoCloseable enclosingClosable = new AutoCloseSpy("enclosingClosable");

		@BeforeAll
		static void setup() {
			enclosingStaticClosable = new AutoCloseSpy("enclosingStaticClosable");
		}

		@Nested
		@TestInstance(PER_CLASS)
		class NestedTestCase implements TestInterface {

			@AutoClose
			static AutoCloseSpy nestedStaticClosable;

			@AutoClose
			final AutoCloseable nestedClosable = new AutoCloseSpy("nestedClosable");

			@BeforeAll
			static void setup() {
				nestedStaticClosable = new AutoCloseSpy("nestedStaticClosable");
			}
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class SuperTestCase {

		@AutoClose
		static AutoCloseable superStaticClosable;

		@AutoClose
		final AutoCloseable superClosable = new AutoCloseSpy("superClosable");

		@BeforeAll
		// WARNING: if this method is named setup() AND the @BeforeAll method in
		// SubTestCase is also named setup(), the latter will "hide" the former.
		static void superSetup() {
			superStaticClosable = new AutoCloseSpy("superStaticClosable");
		}

		@Test
		void superTest() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class SubTestCase extends SuperTestCase {

		@AutoClose
		static AutoCloseable subStaticClosable;

		@AutoClose
		final AutoCloseable subClosable = new AutoCloseSpy("subClosable");

		@BeforeAll
		static void subSetup() {
			subStaticClosable = new AutoCloseSpy("subStaticClosable");
		}

		@Test
		void subTest() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FailingFieldsTestCase {

		@AutoClose
		static AutoCloseable staticField1;

		@AutoClose
		static AutoCloseable staticField2;

		@AutoClose
		static AutoCloseable staticField3;

		@AutoClose
		final AutoCloseable field1 = new AutoCloseSpy("field1", true);

		@AutoClose
		final AutoCloseable field2 = new AutoCloseSpy("field2", true);

		@AutoClose
		final AutoCloseable field3 = new AutoCloseSpy("field3", false);

		@BeforeAll
		static void setup() {
			staticField1 = new AutoCloseSpy("staticField1", true);
			staticField2 = new AutoCloseSpy("staticField2", true);
			staticField3 = new AutoCloseSpy("staticField3", false);
		}

		@Test
		void test() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class FailingFieldsEnclosingTestCase {

		@AutoClose
		static AutoCloseable enclosingStaticField1;

		@AutoClose
		static AutoCloseable enclosingStaticField2;

		@AutoClose
		final AutoCloseable enclosingField1 = new AutoCloseSpy("enclosingField1", true);

		@AutoClose
		final AutoCloseable enclosingField2 = new AutoCloseSpy("enclosingField2", false);

		@BeforeAll
		static void setup() {
			enclosingStaticField1 = new AutoCloseSpy("enclosingStaticField1", true);
			enclosingStaticField2 = new AutoCloseSpy("enclosingStaticField2", false);
		}

		@Test
		void enclosingTest() {
		}

		@Nested
		class NestedTestCase {

			@AutoClose
			static AutoCloseable nestedStaticField1;

			@AutoClose
			static AutoCloseable nestedStaticField2;

			@AutoClose
			final AutoCloseable nestedField1 = new AutoCloseSpy("nestedField1", true);

			@AutoClose
			final AutoCloseable nestedField2 = new AutoCloseSpy("nestedField2", false);

			@BeforeAll
			static void setup() {
				nestedStaticField1 = new AutoCloseSpy("nestedStaticField1", true);
				nestedStaticField2 = new AutoCloseSpy("nestedStaticField2", false);
			}

			@Test
			void nestedTest() {
			}
		}
	}

	@NullUnmarked
	static class AutoCloseSpy implements AutoCloseable, Runnable {

		private final String prefix;
		private final boolean fail;
		private String invokedMethod = null;

		AutoCloseSpy(String prefix) {
			Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
			this.fail = false;
			this.prefix = callerClass.getSimpleName() + "." + prefix + ".";
		}

		AutoCloseSpy(String prefix, boolean fail) {
			Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
			this.prefix = callerClass.getSimpleName() + "." + prefix + ".";
			this.fail = fail;
		}

		@Override
		public void run() {
			recordInvocation("run()");
		}

		@Override
		public void close() {
			recordInvocation("close()");
		}

		private void recordInvocation(String methodName) {
			if (this.invokedMethod != null) {
				throw new IllegalStateException("Already closed via " + this.invokedMethod);
			}
			this.invokedMethod = methodName;
			String invocation = this.prefix + this.invokedMethod;
			recorder.add(invocation);
			if (this.fail) {
				throw new RuntimeException(invocation);
			}
		}
	}

}
