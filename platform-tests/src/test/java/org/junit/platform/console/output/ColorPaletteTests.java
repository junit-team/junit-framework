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

		private static final String ESC = "\u001B[";
		private static final String RESET = ESC + "0m";
		private static final String NEW_LINE = System.lineSeparator();

		@Test
		void verbose_default() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new VerboseTreePrintingListener(out, ColorPalette.DEFAULT, 16,
				Theme.ASCII);

			demoTestRun(listener);

			String output = stringWriter.toString();
			// @formatter:off
			assertThat(output).contains(
				withAnsi("{green}[OK] SUCCESSFUL{NL}{reset}"),
				withAnsi("{red}[X] FAILED{NL}{reset}"),
				withAnsi("{yellow}[A] ABORTED{NL}{reset}"),
				withAnsi("{magenta}[S] SKIPPED")
			);
			// @formatter:on
		}

		@Test
		void verbose_single_color() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new VerboseTreePrintingListener(out, ColorPalette.SINGLE_COLOR, 16,
				Theme.ASCII);

			demoTestRun(listener);

			String output = stringWriter.toString();
			// @formatter:off
			assertThat(output).contains(
				withAnsi("{bold}[OK] SUCCESSFUL{NL}{reset}"),
				withAnsi("{reverse}[X] FAILED{NL}{reset}"),
				withAnsi("{underline}[A] ABORTED{NL}{reset}"),
				withAnsi("{strikethrough}[S] SKIPPED")
			);
			// @formatter:on
		}

		@Test
		void simple_default() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new TreePrintingListener(out, ColorPalette.DEFAULT, Theme.ASCII);

			demoTestRun(listener);

			String output = stringWriter.toString();
			// @formatter:off
			assertThat(output).contains(
				withAnsi("{blue}My Test{reset} {green}[OK]{reset}"),
				withAnsi("{red}My Test{reset} {red}[X]{reset}"),
				withAnsi("{yellow}My Test{reset} {yellow}[A]{reset}"),
				withAnsi("{magenta}My Test{reset} {magenta}[S]{reset}")
			);
			// @formatter:on
		}

		@Test
		void simple_single_color() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new TreePrintingListener(out, ColorPalette.SINGLE_COLOR, Theme.ASCII);

			demoTestRun(listener);

			String output = stringWriter.toString();
			// @formatter:off
			assertThat(output).contains(
				withAnsi("{bold}[OK]{reset}"),
				withAnsi("{reverse}[X]{reset}"),
				withAnsi("{underline}[A]{reset}"),
				withAnsi("{strikethrough}[S]{reset}")
			);
			// @formatter:on
		}

		@Test
		void flat_default() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new FlatPrintingListener(out, ColorPalette.DEFAULT);

			demoTestRun(listener);

			String output = stringWriter.toString();
			// @formatter:off
			assertThat(output).contains(
				withAnsi("{green}Finished:    My Test ([engine:demo-engine]){reset}"),
				withAnsi("{red}Finished:    My Test ([engine:demo-engine]){reset}"),
				withAnsi("{yellow}Finished:    My Test ([engine:demo-engine]){reset}"),
				withAnsi("{magenta}Skipped:     My Test ([engine:demo-engine]){reset}")
			);
			// @formatter:on
		}

		@Test
		void flat_single_color() {
			StringWriter stringWriter = new StringWriter();
			PrintWriter out = new PrintWriter(stringWriter, true);
			TestExecutionListener listener = new FlatPrintingListener(out, ColorPalette.SINGLE_COLOR);

			demoTestRun(listener);

			String output = stringWriter.toString();
			// @formatter:off
			assertThat(output).contains(
				withAnsi("{bold}Finished:    My Test ([engine:demo-engine]){reset}"),
				withAnsi("{reverse}Finished:    My Test ([engine:demo-engine]){reset}"),
				withAnsi("{underline}Finished:    My Test ([engine:demo-engine]){reset}"),
				withAnsi("{strikethrough}Skipped:     My Test ([engine:demo-engine]){reset}")
			);
			// @formatter:on
		}

		private static String withAnsi(String template) {
			// @formatter:off
			return template
					.replace("{blue}", ESC + "34m")
					.replace("{green}", ESC + "32m")
					.replace("{red}", ESC + "31m")
					.replace("{yellow}", ESC + "33m")
					.replace("{magenta}", ESC + "35m")
					.replace("{bold}", ESC + "1m")
					.replace("{reverse}", ESC + "7m")
					.replace("{underline}", ESC + "4m")
					.replace("{strikethrough}", ESC + "9m")
					.replace("{reset}", RESET)
					.replace("{NL}", NEW_LINE);
			// @formatter:on
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
