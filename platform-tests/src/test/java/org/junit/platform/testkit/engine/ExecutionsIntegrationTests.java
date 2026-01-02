/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link Executions}.
 *
 * @since 1.4
 */
class ExecutionsIntegrationTests {

	@Test
	void executionsFromSkippedTestEvents() {
		var testEvents = getTestEvents();

		// We expect 1 for both of the following cases, since an Execution can
		// be created for a "skipped event even if "started" and "finished" events
		// are filtered out.
		assertThat(testEvents.executions().skipped().count()).isEqualTo(1);
		assertThat(testEvents.skipped().executions().count()).isEqualTo(1);
	}

	@Test
	void executionsFromStartedTestEvents() {
		var testEvents = getTestEvents();

		// We expect 3 if the executions are created BEFORE filtering out "finished" events.
		assertThat(testEvents.executions().started().count()).isEqualTo(3);
		// We expect 0 if the executions are created AFTER filtering out "finished" events.
		assertThat(testEvents.started().executions().count()).isEqualTo(0);
	}

	@Test
	void executionsFromFinishedTestEvents() {
		var testEvents = getTestEvents();

		// We expect 3 if the executions are created BEFORE filtering out "started" events.
		assertThat(testEvents.executions().finished().count()).isEqualTo(3);
		// We expect 0 if the executions are created AFTER filtering out "started" events.
		assertThat(testEvents.finished().executions().count()).isEqualTo(0);
	}

	@Test
	void executionsFromSucceededTestEvents() {
		var testEvents = getTestEvents();

		// We expect 1 if the executions are created BEFORE filtering out "finished" events.
		assertThat(testEvents.executions().succeeded().count()).isEqualTo(1);
		// We expect 0 if the executions are created AFTER filtering out "finished" events.
		assertThat(testEvents.succeeded().executions().count()).isEqualTo(0);
	}

	@Test
	void executionsFromAbortedTestEvents() {
		var testEvents = getTestEvents();

		// We expect 1 if the executions are created BEFORE filtering out "started" events.
		assertThat(testEvents.executions().aborted().count()).isEqualTo(1);
		// We expect 0 if the executions are created AFTER filtering out "started" events.
		assertThat(testEvents.aborted().executions().count()).isEqualTo(0);
	}

	@Test
	void executionsFromFailedTestEvents() {
		var testEvents = getTestEvents();

		// We expect 1 if the executions are created BEFORE filtering out "started" events.
		assertThat(testEvents.executions().failed().count()).isEqualTo(1);
		// We expect 0 if the executions are created AFTER filtering out "started" events.
		assertThat(testEvents.failed().executions().count()).isEqualTo(0);
	}

	private Events getTestEvents() {
		return EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(ExampleTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.skipped(1).started(3).succeeded(1).aborted(1).failed(1));
	}

	static class ExampleTestCase {

		@Test
		@Disabled
		void skippedTest() {
		}

		@Test
		void succeedingTest() {
		}

		@Test
		void abortedTest() {
			abort();
		}

		@Test
		void failingTest() {
			fail("Boom!");
		}

	}

}
