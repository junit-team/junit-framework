/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.annotation;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Integration tests that indirectly verify that the {@link Testable @Testable}
 * annotation may be declared on any element type.
 *
 * @since 1.7
 */
class TestableAnnotationTests {

	@Test
	void testMethod() {
		assertNotNull(new TestableEverywhere().toString());
	}

	@Testable
	static class TestableEverywhere {

		@Testable
		final int field = 0;

		@Testable
		TestableEverywhere() {
		}

		@Testable
		void test(@Testable int parameter) {
			@Testable
			@SuppressWarnings("unused")
			var var = "var";
		}
	}

}
