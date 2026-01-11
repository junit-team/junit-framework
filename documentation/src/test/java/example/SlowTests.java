/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

@Tag("exclude")
@Disabled
class SlowTests {

	@Execution(SAME_THREAD)
	@Test
	void a() {
		foo();
	}

	@Test
	void b() {
		foo();
	}

	@Test
	void c() {
		foo();
	}

	@Test
	void d() {
		foo();
	}

	@Test
	void e() {
		foo();
	}

	@Test
	void f() {
		foo();
	}

	@Test
	void g() {
		foo();
	}

	@Test
	void h() {
		foo();
	}

	@Test
	void i() {
		foo();
	}

	@Test
	void j() {
		foo();
	}

	@Test
	void k() {
		foo();
	}

	@Test
	void l() {
		foo();
	}

	@Test
	void m() {
		foo();
	}

	@Test
	void n() {
		foo();
	}

	@Test
	void o() {
		foo();
	}

	@Test
	void p() {
		foo();
	}

	@Execution(SAME_THREAD)
	@Test
	void q() {
		foo();
	}

	@Test
	void r() {
		foo();
	}

	@Test
	void s() {
		foo();
	}

	private void foo() {
		IntStream.range(1, 100_000_000).mapToDouble(i -> Math.pow(i, i)).map(Math::sqrt).max();
	}
}
