/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

// Source: https://github.com/junit-team/junit-framework/issues/3083
@RunWith(Enclosed.class)
public class EnclosedWithParameterizedChildrenJUnit4TestCase {

	@RunWith(Parameterized.class)
	public static class NestedTestCase1 {

		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[] { 1, 2 }, new Object[] { 3, 4 });
		}

		@SuppressWarnings("unused")
		public NestedTestCase1(final int a, final int b) {
		}

		@Test
		public void test() {
		}
	}

	@RunWith(Parameterized.class)
	public static class NestedTestCase2 {

		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[] { 1, 2 }, new Object[] { 3, 4 });
		}

		@SuppressWarnings("unused")
		public NestedTestCase2(final int a, final int b) {
		}

		@Test
		public void test() {
		}
	}
}
