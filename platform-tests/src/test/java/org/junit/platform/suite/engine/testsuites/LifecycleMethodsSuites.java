/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.testsuites;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.engine.BeforeAndAfterSuiteTests;
import org.junit.platform.suite.engine.testcases.StatefulTestCase;

/**
 * Test suites used in {@link BeforeAndAfterSuiteTests}.
 *
 * @since 1.11
 */
@SuppressWarnings("NewClassNamingConvention")
public class LifecycleMethodsSuites {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Suite
	@SelectClasses({ StatefulTestCase.Test1.class, StatefulTestCase.Test2.class })
	private @interface TestSuite {
	}

	@TestSuite
	public static class SuccessfulBeforeAndAfterSuite {

		@BeforeSuite
		static void setUp() {
			StatefulTestCase.callSequence.add("beforeSuiteMethod");
		}

		@AfterSuite
		static void tearDown() {
			StatefulTestCase.callSequence.add("afterSuiteMethod");
		}

	}

	@TestSuite
	public static class FailingBeforeSuite {

		@BeforeSuite
		static void setUp() {
			StatefulTestCase.callSequence.add("beforeSuiteMethod");
			throw new RuntimeException("Exception thrown by @BeforeSuite method");
		}

		@AfterSuite
		static void tearDown() {
			StatefulTestCase.callSequence.add("afterSuiteMethod");
		}

	}

	@TestSuite
	public static class FailingAfterSuite {

		@BeforeSuite
		static void setUp() {
			StatefulTestCase.callSequence.add("beforeSuiteMethod");
		}

		@AfterSuite
		static void tearDown() {
			StatefulTestCase.callSequence.add("afterSuiteMethod");
			throw new RuntimeException("Exception thrown by @AfterSuite method");
		}

	}

	@TestSuite
	public static class FailingBeforeAndAfterSuite {

		@BeforeSuite
		static void setUp() {
			StatefulTestCase.callSequence.add("beforeSuiteMethod");
			throw new RuntimeException("Exception thrown by @BeforeSuite method");
		}

		@AfterSuite
		static void tearDown() {
			StatefulTestCase.callSequence.add("afterSuiteMethod");
			throw new RuntimeException("Exception thrown by @AfterSuite method");
		}

	}

	@TestSuite
	public static class SeveralFailingBeforeAndAfterSuite {

		@BeforeSuite
		static void setUp1() {
			StatefulTestCase.callSequence.add("beforeSuiteMethod");
			throw new RuntimeException("Exception thrown by @BeforeSuite method");
		}

		@BeforeSuite
		static void setUp2() {
			StatefulTestCase.callSequence.add("beforeSuiteMethod");
			throw new RuntimeException("Exception thrown by @BeforeSuite method");
		}

		@AfterSuite
		static void tearDown1() {
			StatefulTestCase.callSequence.add("afterSuiteMethod");
			throw new RuntimeException("Exception thrown by @AfterSuite method");
		}

		@AfterSuite
		static void tearDown2() {
			StatefulTestCase.callSequence.add("afterSuiteMethod");
			throw new RuntimeException("Exception thrown by @AfterSuite method");
		}

	}

	@TestSuite
	public static class SuperclassWithBeforeAndAfterSuite {

		@BeforeSuite
		static void setUp() {
			StatefulTestCase.callSequence.add("superclassBeforeSuiteMethod");
		}

		@AfterSuite
		static void tearDown() {
			StatefulTestCase.callSequence.add("superclassAfterSuiteMethod");
		}

	}

	public static class SubclassWithBeforeAndAfterSuite extends SuperclassWithBeforeAndAfterSuite {

		@BeforeSuite
		static void setUp() {
			StatefulTestCase.callSequence.add("subclassBeforeSuiteMethod");
		}

		@AfterSuite
		static void tearDown() {
			StatefulTestCase.callSequence.add("subclassAfterSuiteMethod");
		}

	}

	@TestSuite
	public static class NonVoidBeforeSuite {

		@BeforeSuite
		static String nonVoidBeforeSuite() {
			fail("Should not be called");
			return "";
		}

	}

	@TestSuite
	public static class ParameterAcceptingBeforeSuite {

		@BeforeSuite
		static void parameterAcceptingBeforeSuite(String param) {
			fail("Should not be called");
		}

	}

	@TestSuite
	public static class NonStaticBeforeSuite {

		@BeforeSuite
		void nonStaticBeforeSuite() {
			fail("Should not be called");
		}

	}

	@TestSuite
	public static class PrivateBeforeSuite {

		@BeforeSuite
		private static void privateBeforeSuite() {
			fail("Should not be called");
		}

	}

	@TestSuite
	public static class NonVoidAfterSuite {

		@AfterSuite
		static String nonVoidAfterSuite() {
			fail("Should not be called");
			return "";
		}

	}

	@TestSuite
	public static class ParameterAcceptingAfterSuite {

		@AfterSuite
		static void parameterAcceptingAfterSuite(String param) {
			fail("Should not be called");
		}

	}

	@TestSuite
	public static class NonStaticAfterSuite {

		@AfterSuite
		void nonStaticAfterSuite() {
			fail("Should not be called");
		}

	}

	@TestSuite
	public static class PrivateAfterSuite {

		@AfterSuite
		private static void privateAfterSuite() {
			fail("Should not be called");
		}

	}

}
