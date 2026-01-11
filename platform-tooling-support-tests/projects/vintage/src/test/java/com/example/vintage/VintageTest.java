/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package com.example.vintage;

import static org.junit.Assert.*;

import org.junit.Test;

public class VintageTest {
	void packagePrivateMethod() {
	}

	@Test
	public void success() {
		// pass
	}

	@Test
	public void failure() {
		fail("expected to fail");
	}
}
