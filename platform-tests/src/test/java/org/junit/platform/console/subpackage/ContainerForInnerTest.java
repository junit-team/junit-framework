/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.subpackage;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Even though our "example test classes" should be named *TestCase
// according to team policy, this class must be named *Test in order
// for ConsoleLauncherIntegrationTests to pass, but that's not a
// problem since the test method here can never fail.
class ContainerForInnerTest {

	@Nested
	class NestedTest {

		@Test
		void test() {
		}
	}

}
