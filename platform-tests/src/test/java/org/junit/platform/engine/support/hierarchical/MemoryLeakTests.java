/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Integration tests intended to verify that memory leaks do not
 * exist with regard to the "context" held by {@link NodeTestTask}.
 *
 * @since 5.3.1
 * @see <a href="https://github.com/junit-team/junit-framework/issues/1578">GitHub issue #1578</a>
 */
// Explicitly specifying Lifecycle.PER_METHOD to be certain that the
// test instance state is recreated for every test method executed.
@TestInstance(Lifecycle.PER_METHOD)
class MemoryLeakTests {

	// Allocate 500 MB of memory per test method.
	//
	// If the test instance is garbage collected, this should not cause any
	// problems for the JUnit build; however, if the instances of this test
	// class are NOT garbage collected, we should run out of memory pretty
	// quickly since the instances of this test class would consume 5GB of
	// heap space.
	final byte[] state = new byte[524_288_000];

	@Test
	void test01() {
	}

	@Test
	void test02() {
	}

	@Test
	void test03() {
	}

	@Test
	void test04() {
	}

	@Test
	void test05() {
	}

	@Test
	void test06() {
	}

	@Test
	void test07() {
	}

	@Test
	void test08() {
	}

	@Test
	void test09() {
	}

	@Test
	void test10() {
	}

}
