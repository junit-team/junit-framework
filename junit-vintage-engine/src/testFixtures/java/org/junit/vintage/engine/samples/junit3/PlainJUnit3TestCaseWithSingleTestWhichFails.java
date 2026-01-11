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

import org.junit.Assert;

/**
 * @since 4.12
 */
public class PlainJUnit3TestCaseWithSingleTestWhichFails extends TestCase {

	public void test() {
		Assert.fail("this test should fail");
	}

}
