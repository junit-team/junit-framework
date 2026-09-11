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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class JUnit4ParallelMethodsTestCase {

	public static class AbstractBlockingTestCase {

		public static final Set<String> threadNames = ConcurrentHashMap.newKeySet();
		public static CyclicBarrier cyclicBarrier;

		@Rule
		public final TestWatcher testWatcher = new TestWatcher() {
			@Override
			protected void starting(Description description) {
				AbstractBlockingTestCase.threadNames.add(Thread.currentThread().getName());
			}
		};

		@Test
		public void fistTest() throws Exception {
			cyclicBarrier.await();
		}

		@Test
		public void secondTest() throws Exception {
			cyclicBarrier.await();
		}

		@Test
		public void thirdTest() throws Exception {
			cyclicBarrier.await();
		}

	}

	public static class FirstMethodTestCase extends JUnit4ParallelMethodsTestCase.AbstractBlockingTestCase {
	}

	public static class SecondMethodTestCase extends JUnit4ParallelMethodsTestCase.AbstractBlockingTestCase {
	}

	public static class ThirdMethodTestCase extends JUnit4ParallelMethodsTestCase.AbstractBlockingTestCase {
	}
}
