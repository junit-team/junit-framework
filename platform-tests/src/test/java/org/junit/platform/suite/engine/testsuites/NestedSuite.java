/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine.testsuites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.engine.testcases.MultipleTestsTestCase;
import org.junit.platform.suite.engine.testcases.SingleTestTestCase;

/**
 * @since 1.8
 */
public class NestedSuite {

	@Suite
	@SelectClasses(SingleTestTestCase.class)
	static class Jupiter {
	}

	@Suite
	@SelectClasses(MultipleTestsTestCase.class)
	static class Tagged {
	}

}
