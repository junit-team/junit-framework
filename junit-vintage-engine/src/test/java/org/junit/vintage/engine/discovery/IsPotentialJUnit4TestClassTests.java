/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IsPotentialJUnit4TestClassTests {

	private final IsPotentialJUnit4TestClass isPotentialJUnit4TestClass = new IsPotentialJUnit4TestClass();

	@Test
	void staticMemberClass() {
		assertTrue(isPotentialJUnit4TestClass.test(Foo.class));
	}

	public static class Foo {
	}

	@Test
	void nonPublicClass() {
		assertFalse(isPotentialJUnit4TestClass.test(Bar.class));
	}

	static class Bar {
	}

	@Test
	void abstractClass() {
		assertFalse(isPotentialJUnit4TestClass.test(Baz.class));
	}

	public static abstract class Baz {
	}

	@Test
	void anonymousClass() {
		var foo = new Foo() {
		};

		assertFalse(isPotentialJUnit4TestClass.test(foo.getClass()));
	}

	public class FooBaz {
	}

	@Test
	void publicInnerClass() {
		assertFalse(isPotentialJUnit4TestClass.test(FooBaz.class));
	}
}
