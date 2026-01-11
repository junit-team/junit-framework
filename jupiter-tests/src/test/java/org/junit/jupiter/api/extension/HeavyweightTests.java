/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Unit tests for {@link org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource}
 * stored values.
 *
 * @since 1.1
 */
class HeavyweightTests {

	@AfterAll
	static void afterAll() {
		Heavyweight.ResourceValue.creations.set(0);
	}

	@Nested
	@TestInstance(PER_CLASS)
	@ExtendWith(Heavyweight.class)
	@ResourceLock(Heavyweight.Resource.ID)
	class Alpha {

		private int mark;

		@BeforeAll
		void setMark(Heavyweight.Resource resource) {
			assertTrue(resource.usages() > 0);
			mark = resource.usages();
		}

		@TestFactory
		Stream<DynamicTest> alpha1(Heavyweight.Resource resource) {
			return Stream.of(dynamicTest("foo", () -> assertTrue(resource.usages() > 1)));
		}

		@Test
		void alpha2(Heavyweight.Resource resource) {
			assertTrue(resource.usages() > 1);
		}

		@Test
		void alpha3(Heavyweight.Resource resource) {
			assertTrue(resource.usages() > 1);
		}

		@AfterAll
		void checkMark(Heavyweight.Resource resource) {
			assertEquals(mark, resource.usages() - 4);
		}

	}

	@Nested
	@TestInstance(PER_CLASS)
	@ExtendWith(Heavyweight.class)
	@ResourceLock(Heavyweight.Resource.ID)
	class Beta {

		private int mark;

		@BeforeAll
		void beforeAll(Heavyweight.Resource resource) {
			assertTrue(resource.usages() > 0);
			mark = resource.usages();
		}

		@BeforeEach
		void beforeEach(Heavyweight.Resource resource) {
			assertTrue(resource.usages() > 1);
		}

		@Test
		void beta(Heavyweight.Resource resource) {
			assertTrue(resource.usages() > 2);
		}

		@AfterAll
		void afterAll(Heavyweight.Resource resource) {
			assertEquals(mark, resource.usages() - 3);
		}

	}

}
