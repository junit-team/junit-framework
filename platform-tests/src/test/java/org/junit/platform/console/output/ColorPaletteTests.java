/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Unit tests for {@link ColorPalette}.
 *
 * @since 1.9
 */
class ColorPaletteTests {

	@Nested
	class LoadFromPropertiesTests {

		@Test
		void singleOverride() {
			String properties = """
					SUCCESSFUL = 35;1
					""";
			ColorPalette colorPalette = new ColorPalette(new StringReader(properties));

			String actual = colorPalette.paint(Style.SUCCESSFUL, "text");

			assertEquals("\u001B[35;1mtext\u001B[0m", actual);
		}

		@Test
		void keysAreCaseInsensitive() {
			String properties = """
					suCcESSfuL = 35;1
					""";
			ColorPalette colorPalette = new ColorPalette(new StringReader(properties));

			String actual = colorPalette.paint(Style.SUCCESSFUL, "text");

			assertEquals("\u001B[35;1mtext\u001B[0m", actual);
		}

		@Test
		void junkKeysAreIgnored() {
			String properties = """
					SUCCESSFUL = 35;1
					JUNK = 1;31;40
					""";
			ColorPalette colorPalette = new ColorPalette(new StringReader(properties));

			String actual = colorPalette.paint(Style.SUCCESSFUL, "text");

			assertEquals("\u001B[35;1mtext\u001B[0m", actual);
		}

		@Test
		void multipleOverrides() {
			String properties = """
					SUCCESSFUL = 35;1
					FAILED = 33;4
					""";
			ColorPalette colorPalette = new ColorPalette(new StringReader(properties));

			String successful = colorPalette.paint(Style.SUCCESSFUL, "text");
			String failed = colorPalette.paint(Style.FAILED, "text");

			assertEquals("\u001B[35;1mtext\u001B[0m", successful);
			assertEquals("\u001B[33;4mtext\u001B[0m", failed);
		}

		@Test
		void unspecifiedStylesAreDefault() {
			String properties = """
					SUCCESSFUL = 35;1
					""";
			ColorPalette colorPalette = new ColorPalette(new StringReader(properties));

			String actual = colorPalette.paint(Style.FAILED, "text");

			assertEquals("\u001B[31mtext\u001B[0m", actual);
		}

		@Test
		void cannotOverrideNone() {
			String properties = """
					NONE = 35;1
					""";
			StringReader reader = new StringReader(properties);

			assertThrows(IllegalArgumentException.class, () -> new ColorPalette(reader));
		}
	}

	@Nested
	class DemonstratePalettesTests {

		private static final String ANSI_ESCAPE = "\u001B[";
		// Default palette ANSI codes
		private static final String SUCCESSFUL_GREEN = ANSI_ESCAPE + "32m";
		private static final String FAILED_RED = ANSI_ESCAPE + "31m";
		private static final String ABORTED_YELLOW = ANSI_ESCAPE + "33m";
		private static final String SKIPPED_MAGENTA = ANSI_ESCAPE + "35m";
		private static final String RESET = ANSI_ESCAPE + "0m";
		// Single color palette ANSI codes
		private static final String BOLD = ANSI_ESCAPE + "1m";
		private static final String REVERSE = ANSI_ESCAPE + "7m";
		private static final String UNDERLINE = ANSI_ESCAPE + "4m";
		private static final String STRIKETHROUGH = ANSI_ESCAPE + "9m";

		@Test
		void verbose_default() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new VerboseTreePrintingListener(out, ColorPalette.DEFAULT, 16,
				Theme.ASCII);

			demoTestRun(listener);

			String output = stringWriter.toString();
			assertThat(output).contains(
					"My Test",
					SUCCESSFUL_GREEN,
					FAILED_RED,
					ABORTED_YELLOW,
					SKIPPED_MAGENTA,
					RESET);
		}

		@Test
		void verbose_single_color() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new VerboseTreePrintingListener(out, ColorPalette.SINGLE_COLOR, 16,
				Theme.ASCII);

			demoTestRun(listener);

			String output = stringWriter.toString();
			assertThat(output).contains(
					"My Test",
					BOLD,
					REVERSE,
					UNDERLINE,
					STRIKETHROUGH,
					RESET);
		}

		@Test
		void simple_default() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new TreePrintingListener(out, ColorPalette.DEFAULT, Theme.ASCII);

			demoTestRun(listener);

			String output = stringWriter.toString();
			assertThat(output).contains(
					"My Test",
					SUCCESSFUL_GREEN,
					FAILED_RED,
					ABORTED_YELLOW,
					SKIPPED_MAGENTA,
					RESET);
		}

		@Test
		void simple_single_color() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new TreePrintingListener(out, ColorPalette.SINGLE_COLOR, Theme.ASCII);

			demoTestRun(listener);

			String output = stringWriter.toString();
			assertThat(output).contains(
					"My Test",
					BOLD,
					REVERSE,
					UNDERLINE,
					STRIKETHROUGH,
					RESET);
		}

		@Test
		void flat_default() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new FlatPrintingListener(out, ColorPalette.DEFAULT);

			demoTestRun(listener);

			String output = stringWriter.toString();
			assertThat(output).contains(
					"My Test",
					SUCCESSFUL_GREEN,
					FAILED_RED,
					ABORTED_YELLOW,
					SKIPPED_MAGENTA,
					RESET);
		}

		@Test
		void flat_single_color() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new FlatPrintingListener(out, ColorPalette.SINGLE_COLOR);

			demoTestRun(listener);

			String output = stringWriter.toString();
			assertThat(output).contains(
					"My Test",
					BOLD,
					REVERSE,
					UNDERLINE,
					STRIKETHROUGH,
					RESET);
		}

		private void demoTestRun(TestExecutionListener listener) {
			TestDescriptor testDescriptor = new TestDescriptorStub(UniqueId.forEngine("demo-engine"), "My Test");
			TestPlan testPlan = TestPlan.from(true, List.of(testDescriptor), mock(), dummyOutputDirectoryCreator());
			listener.testPlanExecutionStarted(testPlan);
			listener.executionStarted(TestIdentifier.from(testDescriptor));
			listener.executionFinished(TestIdentifier.from(testDescriptor), TestExecutionResult.successful());
			listener.dynamicTestRegistered(TestIdentifier.from(testDescriptor));
			listener.executionStarted(TestIdentifier.from(testDescriptor));
			listener.executionFinished(TestIdentifier.from(testDescriptor),
				TestExecutionResult.failed(new Exception()));
			listener.executionStarted(TestIdentifier.from(testDescriptor));
			listener.executionFinished(TestIdentifier.from(testDescriptor),
				TestExecutionResult.aborted(new Exception()));
			listener.reportingEntryPublished(TestIdentifier.from(testDescriptor), ReportEntry.from("Key", "Value"));
			listener.executionSkipped(TestIdentifier.from(testDescriptor), "to demonstrate skipping");
			listener.testPlanExecutionFinished(testPlan);
		}

	}

}
