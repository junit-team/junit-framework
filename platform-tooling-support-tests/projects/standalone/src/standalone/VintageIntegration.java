/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package standalone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;

@FixMethodOrder(NAME_ASCENDING)
public class VintageIntegration {

	@Test
	@Ignore("integr4tion test")
	public void ignored() {
		fail("this test should be ignored");
	}

	@Test
	public void succ3ssful() {
		assertEquals(3, 1 + 2);
	}

	@Test
	public void f4il() {
		fail("f4iled");
	}
}
