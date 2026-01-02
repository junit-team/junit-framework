/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import java.util.Arrays;

import org.junit.jupiter.params.AfterParameterizedClassInvocation;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

public class ParameterizedMigrationDemo {

	// tag::before[]
	@RunWith(Parameterized.class)
	// end::before[]
	static
	// tag::before[]
	public class JUnit4ParameterizedClassTests {

		@Parameterized.Parameters
		public static Iterable<Object[]> data() {
			return Arrays.asList(new Object[][] { { 1, "foo" }, { 2, "bar" } });
		}

		// end::before[]
		@SuppressWarnings("DefaultAnnotationParam")
		// tag::before[]
		@Parameterized.Parameter(0)
		public int number;

		@Parameterized.Parameter(1)
		public String text;

		@Parameterized.BeforeParam
		public static void before(int number, String text) {
		}

		@Parameterized.AfterParam
		public static void after() {
		}

		@org.junit.Test
		public void someTest() {
		}

		@org.junit.Test
		public void anotherTest() {
		}
	}
	// end::before[]

	// tag::after[]
	@ParameterizedClass
	@MethodSource("data")
	// end::after[]
	static
	// tag::after[]
	class JupiterParameterizedClassTests {

		static Iterable<Object[]> data() {
			return Arrays.asList(new Object[][] { { 1, "foo" }, { 2, "bar" } });
		}

		@org.junit.jupiter.params.Parameter(0)
		int number;

		@org.junit.jupiter.params.Parameter(1)
		String text;

		@BeforeParameterizedClassInvocation
		static void before(int number, String text) {
		}

		@AfterParameterizedClassInvocation
		static void after() {
		}

		@org.junit.jupiter.api.Test
		void someTest() {
		}

		@org.junit.jupiter.api.Test
		void anotherTest() {
		}
	}
	// end::after[]

}
