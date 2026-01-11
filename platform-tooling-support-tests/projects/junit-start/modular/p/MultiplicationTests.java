/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package p;

import module org.junit.jupiter.api;

class MultiplicationTests {

	@Test
	void multiplication() {
		Assertions.assertEquals(4, 2 * 2, "Multiplication error detected!");
	}
}
