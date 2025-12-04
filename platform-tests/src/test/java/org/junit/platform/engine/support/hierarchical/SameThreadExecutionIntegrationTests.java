/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * @since 1.4
 */
class SameThreadExecutionIntegrationTests {

	/**
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/1688">gh-1688</a>
	 */
	@Test
	void threadInterruptedByUserCode(@TrackLogRecords LogRecordListener listener) {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(InterruptedThreadTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.succeeded(4));

		assertThat(firstDebugLogRecord(listener).getMessage()).matches(
			"Execution of TestDescriptor with display name .+test1.+ and "
					+ "unique ID .+ failed to clear the 'interrupted status' flag "
					+ "for the current thread. JUnit has cleared the flag, but you "
					+ "may wish to investigate why the flag was not cleared by user code.");
	}

	private LogRecord firstDebugLogRecord(LogRecordListener listener) throws AssertionError {
		return listener.stream(NodeTestTask.class, Level.FINE).findFirst().orElseThrow(
			() -> new AssertionError("Failed to find debug log record"));
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	@TestMethodOrder(MethodName.class)
	static class InterruptedThreadTestCase {

		@Test
		void test1() {
			Thread.currentThread().interrupt();
		}

		@Test
		void test2() throws InterruptedException {
			Thread.sleep(10);
		}

		@Test
		void test3() {
			Thread.currentThread().interrupt();
		}

		@Test
		void test4() throws InterruptedException {
			Thread.sleep(10);
		}

	}

}
