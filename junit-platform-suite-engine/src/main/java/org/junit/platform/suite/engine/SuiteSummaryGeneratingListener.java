/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.engine;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/**
 * A minimal implementation of the {@link SummaryGeneratingListener}.
 * <p>
 * The {@code SummaryGeneratingListener} assumes that all the ancestors all
 * test descriptors are in the test plan. This isn't true for the suite engine
 * which only executes a subtree of the test plan. This implementation tracks
 * only the essentials.
 */
final class SuiteSummaryGeneratingListener implements TestExecutionListener {
	private final AtomicLong testsFound = new AtomicLong();

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		this.testsFound.set(testPlan.countTestIdentifiers(TestIdentifier::isTest));
	}

	public long getTestsFoundCount() {
		return testsFound.get();
	}
}
