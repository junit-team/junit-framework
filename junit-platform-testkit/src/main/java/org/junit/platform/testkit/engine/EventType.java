/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static org.apiguardian.api.API.Status.MAINTAINED;

import org.apiguardian.api.API;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;

/**
 * Enumeration of the different possible {@link Event} types.
 *
 * @since 1.4
 * @see Event
 */
@API(status = MAINTAINED, since = "1.7")
public enum EventType {

	/**
	 * Signals that a {@link TestDescriptor} has been dynamically registered.
	 *
	 * @see org.junit.platform.engine.EngineExecutionListener#dynamicTestRegistered(TestDescriptor)
	 */
	DYNAMIC_TEST_REGISTERED,

	/**
	 * Signals that the execution of a {@link TestDescriptor} has been skipped.
	 *
	 * @see org.junit.platform.engine.EngineExecutionListener#executionSkipped(TestDescriptor, String)
	 */
	SKIPPED,

	/**
	 * Signals that the execution of a {@link TestDescriptor} has started.
	 *
	 * @see org.junit.platform.engine.EngineExecutionListener#executionStarted(TestDescriptor)
	 */
	STARTED,

	/**
	 * Signals that the execution of a {@link TestDescriptor} has finished,
	 * regardless of the outcome.
	 *
	 * @see org.junit.platform.engine.EngineExecutionListener#executionFinished(TestDescriptor, TestExecutionResult)
	 */
	FINISHED,

	/**
	 * Signals that a {@link TestDescriptor} published a reporting entry.
	 *
	 * @see org.junit.platform.engine.EngineExecutionListener#reportingEntryPublished(TestDescriptor, ReportEntry)
	 */
	REPORTING_ENTRY_PUBLISHED,

	/**
	 * Signals that a {@link TestDescriptor} published a file entry.
	 *
	 * @since 1.12
	 * @see org.junit.platform.engine.EngineExecutionListener#fileEntryPublished(TestDescriptor, FileEntry)
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	FILE_ENTRY_PUBLISHED

}
