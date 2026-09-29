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

import static java.util.Objects.requireNonNull;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.ExceptionUtils.readStackTrace;
import static org.junit.platform.console.output.Style.NONE;

import java.io.PrintWriter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.14")
public class VerboseTreePrintingListener implements DetailsPrintingListener {

	private final PrintWriter out;
	private final Theme theme;
	private final ColorPalette colorPalette;
	private final Clock clock;
	private final String[] verticals;
	private final Map<UniqueId, Instant> startInstantByUniqueId = new ConcurrentHashMap<>();

	private @Nullable TestPlan testPlan;

	public VerboseTreePrintingListener(PrintWriter out, ColorPalette colorPalette, int maxContainerNestingLevel,
			Theme theme) {
		this(out, colorPalette, maxContainerNestingLevel, theme, Clock.systemUTC());
	}

	VerboseTreePrintingListener(PrintWriter out, ColorPalette colorPalette, int maxContainerNestingLevel, Theme theme,
			Clock clock) {
		this.out = out;
		this.colorPalette = colorPalette;
		this.theme = theme;
		this.clock = clock;

		// create and populate vertical indentation lookup table, indexed by nesting level
		this.verticals = new String[Math.max(10, maxContainerNestingLevel) + 1];
		this.verticals[0] = ""; // "engine" level
		for (int i = 1; i < verticals.length; i++) {
			verticals[i] = verticals[i - 1] + theme.vertical();
		}
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		this.testPlan = testPlan;

		StringBuilder output = new StringBuilder();
		String prefix = "Test plan execution started. Number of static tests: ";
		appendNumberOfTests(output, testPlan, prefix);
		append(output, Style.CONTAINER, "%s%n", theme.root());
		print(output);
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		StringBuilder output = new StringBuilder();
		appendNumberOfTests(output, testPlan, "Test plan execution finished. Number of all tests: ");
		print(output);
	}

	private void appendNumberOfTests(StringBuilder output, TestPlan testPlan, String prefix) {
		long tests = testPlan.countTestIdentifiers(TestIdentifier::isTest);
		append(output, NONE, "%s", prefix);
		append(output, Style.TEST, "%d%n", tests);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		startInstantByUniqueId.put(testIdentifier.getUniqueIdObject(), clock.instant());
		int nestingLevel = nestingLevel(testIdentifier);
		StringBuilder output = new StringBuilder();
		if (testIdentifier.isContainer()) {
			appendVerticals(output, nestingLevel, theme.entry());
			append(output, Style.CONTAINER, " %s", testIdentifier.getDisplayName());
			append(output, NONE, "%n");
			print(output);
			return;
		}
		appendVerticals(output, nestingLevel, theme.entry());
		append(output, Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
		appendDetails(output, nestingLevel, testIdentifier);
		print(output);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		long durationMillis = durationMillis(testIdentifier);
		int nestingLevel = nestingLevel(testIdentifier);
		StringBuilder output = new StringBuilder();
		testExecutionResult.getThrowable().ifPresent(
			t -> appendDetail(output, nestingLevel, Style.FAILED, "caught", readStackTrace(t)));
		if (testIdentifier.isContainer()) {
			appendVerticals(output, nestingLevel, theme.end());
			append(output, Style.CONTAINER, " %s", testIdentifier.getDisplayName());
			append(output, NONE, " finished after %d ms.%n", durationMillis);
			print(output);
			return;
		}
		appendDetail(output, nestingLevel, NONE, "duration", "%d ms%n", durationMillis);
		String status = theme.status(testExecutionResult) + " " + testExecutionResult.getStatus();
		appendDetail(output, nestingLevel, Style.valueOf(testExecutionResult), "status", "%s%n", status);
		print(output);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		int nestingLevel = nestingLevel(testIdentifier);
		StringBuilder output = new StringBuilder();
		appendVerticals(output, nestingLevel, theme.entry());
		append(output, Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
		appendDetails(output, nestingLevel, testIdentifier);
		appendDetail(output, nestingLevel, Style.SKIPPED, "reason", reason);
		appendDetail(output, nestingLevel, Style.SKIPPED, "status", theme.skipped() + " SKIPPED");
		print(output);
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		StringBuilder output = new StringBuilder();
		appendVerticals(output, nestingLevel(testIdentifier), theme.entry());
		append(output, Style.DYNAMIC, " %s", testIdentifier.getDisplayName());
		append(output, NONE, "%s%n", " dynamically registered");
		print(output);
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		StringBuilder output = new StringBuilder();
		appendDetail(output, nestingLevel(testIdentifier), Style.REPORTED, "reports", entry.toString());
		print(output);
	}

	@Override
	public void fileEntryPublished(TestIdentifier testIdentifier, FileEntry file) {
		StringBuilder output = new StringBuilder();
		appendDetail(output, nestingLevel(testIdentifier), Style.REPORTED, "reports", file.toString());
		print(output);
	}

	/**
	 * Append static information about the test identifier.
	 */
	private void appendDetails(StringBuilder output, int nestingLevel, TestIdentifier testIdentifier) {
		appendDetail(output, nestingLevel, NONE, "tags", "%s%n", testIdentifier.getTags());
		appendDetail(output, nestingLevel, NONE, "uniqueId", "%s%n", testIdentifier.getUniqueId());
		appendDetail(output, nestingLevel, NONE, "parent", "%s%n", testIdentifier.getParentId().orElse("[]"));
		testIdentifier.getSource().ifPresent(
			source -> appendDetail(output, nestingLevel, NONE, "source", "%s%n", source));
	}

	/**
	 * Determine the elapsed time since the execution of the supplied test
	 * identifier started.
	 */
	private long durationMillis(TestIdentifier testIdentifier) {
		var uniqueId = testIdentifier.getUniqueIdObject();
		var startInstant = requireNonNull(startInstantByUniqueId.remove(uniqueId));
		return Duration.between(startInstant, clock.instant()).toMillis();
	}

	/**
	 * Determine the nesting level of the supplied test identifier, i.e. the number
	 * of its ancestors in the test plan, with test engines being at level 0.
	 */
	private int nestingLevel(TestIdentifier testIdentifier) {
		return nestingLevel(requireNonNull(testPlan), testIdentifier);
	}

	private int nestingLevel(TestPlan testPlan, TestIdentifier testIdentifier) {
		int nestingLevel = 0;
		TestIdentifier current = testIdentifier;
		TestIdentifier parent;
		while ((parent = testPlan.getParent(current).orElse(null)) != null) {
			current = parent;
			nestingLevel++;
		}
		return nestingLevel;
	}

	private String verticals(int nestingLevel) {
		return verticals[Math.min(nestingLevel, verticals.length - 1)];
	}

	private void appendVerticals(StringBuilder output, int nestingLevel, String tile) {
		append(output, NONE, verticals(nestingLevel));
		append(output, NONE, tile);
	}

	private void append(StringBuilder output, Style style, String message, Object... args) {
		output.append(colorPalette.paint(style, message).formatted(args));
	}

	/**
	 * Write the output of a single event with a single write so that events printed
	 * concurrently cannot interleave within a line.
	 */
	private void print(StringBuilder output) {
		out.print(output);
		out.flush();
	}

	/**
	 * Append single detail with a potential multi-line message.
	 */
	private void appendDetail(StringBuilder output, int nestingLevel, Style style, String detail, String format,
			Object... args) {
		// append initial verticals - expecting to be at start of the line
		String verticals = verticals(nestingLevel + 1);
		append(output, NONE, verticals);
		String detailFormat = "%9s";
		// omit detail string if it's empty
		if (!detail.isEmpty()) {
			append(output, NONE, "%s", (detailFormat + ": ").formatted(detail));
		}
		// trivial case: at least one arg is given? Let the format do the entire work
		if (args.length > 0) {
			append(output, style, format, args);
			return;
		}
		// still here? Split format into separate lines and indent them from the second line on
		String[] lines = format.split("\\R");
		append(output, style, "%s", lines[0]);
		if (lines.length > 1) {
			String delimiter = System.lineSeparator() + verticals + (detailFormat + "    ").formatted("");
			for (int i = 1; i < lines.length; i++) {
				append(output, NONE, "%s", delimiter);
				append(output, style, "%s", lines[i]);
			}
		}
		append(output, NONE, "%n");
	}

	@Override
	public void listTests(TestPlan testPlan) {
		testPlan.accept(new TestPlan.Visitor() {
			@Override
			public void preVisitContainer(TestIdentifier testIdentifier) {
				if (!testPlan.getChildren(testIdentifier).isEmpty()) {
					StringBuilder output = new StringBuilder();
					appendVerticals(output, nestingLevel(testPlan, testIdentifier), theme.entry());
					append(output, Style.CONTAINER, " %s", testIdentifier.getDisplayName());
					append(output, NONE, "%n");
					print(output);
				}
			}

			@Override
			public void visit(TestIdentifier testIdentifier) {
				if (testPlan.getChildren(testIdentifier).isEmpty()) {
					int nestingLevel = nestingLevel(testPlan, testIdentifier);
					StringBuilder output = new StringBuilder();
					appendVerticals(output, nestingLevel, theme.entry());
					append(output, Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
					appendDetails(output, nestingLevel, testIdentifier);
					print(output);
				}
			}

			@Override
			public void postVisitContainer(TestIdentifier testIdentifier) {
				if (!testPlan.getChildren(testIdentifier).isEmpty()) {
					StringBuilder output = new StringBuilder();
					appendVerticals(output, nestingLevel(testPlan, testIdentifier), theme.end());
					append(output, Style.CONTAINER, " %s%n", testIdentifier.getDisplayName());
					print(output);
				}
			}
		});
	}
}
