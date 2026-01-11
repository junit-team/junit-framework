/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.0
 */
public class NoopTestExecutionListener implements TestExecutionListener {
	public static boolean called;

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		called = true;
	}
}
