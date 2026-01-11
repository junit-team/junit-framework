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

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.platform.engine.CancellationToken;

public class CancellingTestCase {

	public static CancellationToken cancellationToken;

	@Before
	public void cancelExecution() {
		requireNonNull(cancellationToken).cancel();
	}

	@Test
	public void first() {
		fail();
	}

	@Test
	public void second() {
		fail();
	}
}
