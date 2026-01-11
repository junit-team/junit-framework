/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

import module org.junit.start;

void main() {
	JUnit.run();
}

@Test
void addition() {
	Assertions.assertEquals(2, 1 + 1, "Addition error detected!");
}
