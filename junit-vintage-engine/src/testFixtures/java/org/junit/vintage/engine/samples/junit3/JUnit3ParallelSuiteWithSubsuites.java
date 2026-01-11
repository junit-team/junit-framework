/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit3;

import junit.extensions.ActiveTestSuite;
import junit.framework.TestCase;
import junit.framework.TestSuite;

@SuppressWarnings("JUnitMalformedDeclaration")
public class JUnit3ParallelSuiteWithSubsuites extends TestCase {
	private final String arg;

	public JUnit3ParallelSuiteWithSubsuites(String name, String arg) {
		super(name);
		this.arg = arg;
	}

	public void hello() {
		assertNotNull(arg);
	}

	public static TestSuite suite() {
		TestSuite root = new ActiveTestSuite("allTests");
		var case1 = new TestSuite("Case1");
		case1.addTest(new JUnit3ParallelSuiteWithSubsuites("hello", "world"));
		root.addTest(case1);
		var case2 = new TestSuite("Case2");
		case2.addTest(new JUnit3ParallelSuiteWithSubsuites("hello", "WORLD"));
		root.addTest(case2);
		return root;
	}
}
