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

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @since 5.4.1
 */
@RunWith(Parameterized.class)
public class IgnoredParameterizedTestCase {

	@Parameters(name = "{0}")
	public static Iterable<String> parameters() {
		return List.of("foo", "bar");
	}

	@Parameter
	public String value;

	@Test
	@Ignore
	public void test() {
		// never called
	}

}
