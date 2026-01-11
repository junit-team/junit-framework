/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.support;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

/**
 * Factory for creating {@link ThrowableCollector ThrowableCollectors} within
 * the JUnit Jupiter test engine.
 *
 * @since 5.4
 * @see ThrowableCollector
 */
@API(status = INTERNAL, since = "5.4")
public class JupiterThrowableCollectorFactory {

	/**
	 * Create a new {@link ThrowableCollector} that treats instances of the
	 * OTA's {@link org.opentest4j.TestAbortedException} and JUnit 4's
	 * {@code org.junit.AssumptionViolatedException} as <em>aborting</em>.
	 */
	public static ThrowableCollector createThrowableCollector() {
		return new OpenTest4JAndJUnit4AwareThrowableCollector();
	}

	private JupiterThrowableCollectorFactory() {
	}

}
