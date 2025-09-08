/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;

/**
 * Listener to be notified of test execution events by
 * {@linkplain TestEngine test engines}.
 *
 * <p>Contrary to JUnit 4, {@linkplain TestEngine test engines}
 * must report events not only for {@linkplain TestDescriptor test descriptors}
 * that represent executable leaves but also for all intermediate containers.
 *
 * @since 1.0
 * @see TestEngine
 * @see ExecutionRequest
 */
@API(status = STABLE, since = "1.0")
public interface EngineExecutionListener {

	/**
	 * No-op implementation of {@code EngineExecutionListener}
	 */
	EngineExecutionListener NOOP = new EngineExecutionListener() {
	};

	/**
	 * Must be called when a new, dynamic {@link TestDescriptor} has been
	 * registered.
	 *
	 * <p>A <em>dynamic test</em> is a test that is not known a-priori and
	 * therefore was not present in the test tree when discovering tests.
	 *
	 * @param testDescriptor the descriptor of the newly registered test
	 * or container
	 */
	default void dynamicTestRegistered(TestDescriptor testDescriptor) {
	}

	/**
	 * Must be called when the execution of a leaf or subtree of the test tree
	 * has been skipped.
	 *
	 * <p>The {@link TestDescriptor} may represent a test or a container. In the
	 * case of a container, an engine must not fire any additional events for its
	 * descendants.
	 *
	 * <p>A skipped test or subtree of tests must not be reported as
	 * {@linkplain #executionStarted started} or
	 * {@linkplain #executionFinished finished}.
	 *
	 * @param testDescriptor the descriptor of the skipped test or container
	 * @param reason a human-readable message describing why the execution
	 * has been skipped
	 */
	default void executionSkipped(TestDescriptor testDescriptor, String reason) {
	}

	/**
	 * Must be called when the execution of a leaf or subtree of the test tree
	 * is about to be started.
	 *
	 * <p>The {@link TestDescriptor} may represent a test or a container. In the
	 * case of a container, an engine must fire additional events for its
	 * children.
	 *
	 * <p>This method may only be called if the test or container has not been
	 * {@linkplain #executionSkipped skipped}.
	 *
	 * <p>This method must be called for a container {@code TestDescriptor}
	 * <em>before</em> {@linkplain #executionStarted starting} or
	 * {@linkplain #executionSkipped skipping} any of its children.
	 *
	 * @param testDescriptor the descriptor of the started test or container
	 */
	default void executionStarted(TestDescriptor testDescriptor) {
	}

	/**
	 * Must be called when the execution of a leaf or subtree of the test tree
	 * has finished, regardless of the outcome.
	 *
	 * <p>The {@link TestDescriptor} may represent a test or a container.
	 *
	 * <p>This method may only be called if the test or container has not
	 * been {@linkplain #executionSkipped skipped}.
	 *
	 * <p>This method must be called for a container {@code TestIdentifier}
	 * <em>after</em> all of its children have been
	 * {@linkplain #executionSkipped skipped} or have
	 * {@linkplain #executionFinished finished}.
	 *
	 * <p>The {@link TestExecutionResult} describes the result of the execution
	 * for the supplied {@code testDescriptor}. The result does not include or
	 * aggregate the results of its children. For example, a container with a
	 * failing test must be reported as {@link Status#SUCCESSFUL SUCCESSFUL} even
	 * if one or more of its children are reported as {@link Status#FAILED FAILED}.
	 *
	 * @param testDescriptor the descriptor of the finished test or container
	 * @param testExecutionResult the (unaggregated) result of the execution for
	 * the supplied {@code TestDescriptor}
	 *
	 * @see TestExecutionResult
	 */
	default void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
	}

	/**
	 * Can be called for any {@link TestDescriptor} in order to publish additional
	 * information to the reporting infrastructure &mdash; for example:
	 *
	 * <ul>
	 *     <li>Output that would otherwise go to {@code System.out}</li>
	 *     <li>Information about test context or test data</li>
	 * </ul>
	 *
	 * <p>The current lifecycle state of the supplied {@code TestDescriptor} is
	 * not relevant: reporting events can occur at any time.
	 *
	 * @param testDescriptor the descriptor of the test or container to which
	 * the reporting entry belongs
	 * @param reportEntry the {@link ReportEntry} to be published
	 */
	default void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry reportEntry) {
	}

	/**
	 * Can be called for any {@link TestDescriptor} in order to publish a file or
	 * directory by attaching it to a test or container &mdash; for example:
	 *
	 * <ul>
	 *     <li>Screenshots</li>
	 *     <li>Logs</li>
	 *     <li>Output files written by the code under test</li>
	 *     <li>Test output directory</li>
	 * </ul>
	 *
	 * <p>The current lifecycle state of the supplied {@code TestDescriptor} is
	 * not relevant: file events can occur at any time.
	 *
	 * @param testDescriptor the descriptor of the test or container to which
	 * the file entry belongs
	 * @param fileEntry the {@link FileEntry} to be published
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	default void fileEntryPublished(TestDescriptor testDescriptor, FileEntry fileEntry) {
	}

}
