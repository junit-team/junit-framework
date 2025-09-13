/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.jfr;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.hierarchicalOutputDirectoryCreator;
import static org.junit.platform.reporting.testutil.FileUtils.findPath;
import static org.moditect.jfrunit.ExpectedEvent.event;
import static org.moditect.jfrunit.JfrEventsAssert.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.DisabledOnOpenJ9;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

@JfrEventTest
@DisabledOnOpenJ9
public class FlightRecordingExecutionListenerIntegrationTests {

	public JfrEvents jfrEvents = new JfrEvents();

	@Test
	@EnableEvent("org.junit.*")
	void reportsEvents(@TempDir Path tempDir) {
		var launcher = LauncherFactoryForTestingPurposesOnly.createLauncher(new JupiterTestEngine());
		var request = request() //
				.selectors(selectClass(TestCase.class)) //
				.outputDirectoryCreator(hierarchicalOutputDirectoryCreator(tempDir)) //
				.forExecution() //
				.build();

		launcher.execute(request);
		jfrEvents.awaitEvents();

		var testFile = findPath(tempDir, "glob:**/test.txt");

		assertThat(jfrEvents) //
				.contains(event("org.junit.TestPlanExecution") //
						.with("engineNames", "JUnit Jupiter")) //
				.contains(event("org.junit.TestExecution") //
						.with("displayName", "JUnit Jupiter") //
						.with("type", "CONTAINER")) //
				.contains(event("org.junit.TestExecution") //
						.with("displayName", FlightRecordingExecutionListenerIntegrationTests.class.getSimpleName()
								+ "$" + TestCase.class.getSimpleName()) //
						.with("type", "CONTAINER")) //
				.contains(event("org.junit.TestExecution") //
						.with("displayName", "test(TestReporter)") //
						.with("type", "TEST") //
						.with("result", "SUCCESSFUL")) //
				.contains(event("org.junit.ReportEntry") //
						.with("key", "message") //
						.with("value", "Hello JFR!")) //
				.contains(event("org.junit.FileEntry") //
						.with("path", testFile.toAbsolutePath().toString())) //
				.contains(event("org.junit.SkippedTest") //
						.with("displayName", "skipped()") //
						.with("type", "TEST") //
						.with("reason", "for demonstration purposes"));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCase {
		@Test
		void test(TestReporter reporter) {
			reporter.publishEntry("message", "Hello JFR!");
			reporter.publishFile("test.txt", MediaType.TEXT_PLAIN_UTF_8, file -> Files.writeString(file, "test"));
		}

		@Test
		@Disabled("for demonstration purposes")
		void skipped() {
		}
	}
}
