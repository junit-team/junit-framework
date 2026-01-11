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

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @since 4.12
 */
public class JUnit3SuiteWithSingleTestCaseWithSingleTestWhichFails extends TestCase {

	public static junit.framework.Test suite() {
		var suite = new TestSuite();
		suite.addTestSuite(PlainJUnit3TestCaseWithSingleTestWhichFails.class);
		return suite;
	}

}
