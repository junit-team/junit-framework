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
import java.util.Map;
import java.util.Optional;
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
	private final String[] verticals;
	private final Map<UniqueId, Long> startedMillisByUniqueId = new ConcurrentHashMap<>();

	private @Nullable TestPlan testPlan;

	public VerboseTreePrintingListener(PrintWriter out, ColorPalette colorPalette, int maxContainerNestingLevel,
			Theme theme) {
		this.out = out;
		this.colorPalette = colorPalette;
		this.theme = theme;

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

		String prefix = "Test plan execution started. Number of static tests: ";
		printNumberOfTests(testPlan, prefix);
		printf(Style.CONTAINER, "%s%n", theme.root());
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		printNumberOfTests(testPlan, "Test plan execution finished. Number of all tests: ");
	}

	private void printNumberOfTests(TestPlan testPlan, String prefix) {
		long tests = testPlan.countTestIdentifiers(TestIdentifier::isTest);
		printf(NONE, "%s", prefix);
		printf(Style.TEST, "%d%n", tests);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		startedMillisByUniqueId.put(testIdentifier.getUniqueIdObject(), System.currentTimeMillis());
		int nestingLevel = nestingLevel(testIdentifier);
		printVerticals(nestingLevel, theme.entry());
		if (testIdentifier.isContainer()) {
			printf(Style.CONTAINER, " %s", testIdentifier.getDisplayName());
			printf(NONE, "%n");
			return;
		}
		printf(Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
		printDetails(nestingLevel, testIdentifier);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		long startedMillis = requireNonNull(startedMillisByUniqueId.remove(testIdentifier.getUniqueIdObject()));
		int nestingLevel = nestingLevel(testIdentifier);
		testExecutionResult.getThrowable().ifPresent(
			t -> printDetail(nestingLevel, Style.FAILED, "caught", readStackTrace(t)));
		if (testIdentifier.isContainer()) {
			printVerticals(nestingLevel, theme.end());
			printf(Style.CONTAINER, " %s", testIdentifier.getDisplayName());
			printf(NONE, " finished after %d ms.%n", System.currentTimeMillis() - startedMillis);
			return;
		}
		printDetail(nestingLevel, NONE, "duration", "%d ms%n", System.currentTimeMillis() - startedMillis);
		String status = theme.status(testExecutionResult) + " " + testExecutionResult.getStatus();
		printDetail(nestingLevel, Style.valueOf(testExecutionResult), "status", "%s%n", status);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		int nestingLevel = nestingLevel(testIdentifier);
		printVerticals(nestingLevel, theme.entry());
		printf(Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
		printDetails(nestingLevel, testIdentifier);
		printDetail(nestingLevel, Style.SKIPPED, "reason", reason);
		printDetail(nestingLevel, Style.SKIPPED, "status", theme.skipped() + " SKIPPED");
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		printVerticals(nestingLevel(testIdentifier), theme.entry());
		printf(Style.DYNAMIC, " %s", testIdentifier.getDisplayName());
		printf(NONE, "%s%n", " dynamically registered");
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		printDetail(nestingLevel(testIdentifier), Style.REPORTED, "reports", entry.toString());
	}

	@Override
	public void fileEntryPublished(TestIdentifier testIdentifier, FileEntry file) {
		printDetail(nestingLevel(testIdentifier), Style.REPORTED, "reports", file.toString());
	}

	/**
	 * Print static information about the test identifier.
	 */
	private void printDetails(int nestingLevel, TestIdentifier testIdentifier) {
		printDetail(nestingLevel, NONE, "tags", "%s%n", testIdentifier.getTags());
		printDetail(nestingLevel, NONE, "uniqueId", "%s%n", testIdentifier.getUniqueId());
		printDetail(nestingLevel, NONE, "parent", "%s%n", testIdentifier.getParentId().orElse("[]"));
		testIdentifier.getSource().ifPresent(source -> printDetail(nestingLevel, NONE, "source", "%s%n", source));
	}

	/**
	 * Determine the nesting level of the supplied test identifier, i.e. the number
	 * of its ancestors in the test plan, with test engines being at level 0.
	 */
	private int nestingLevel(TestIdentifier testIdentifier) {
		TestPlan testPlan = requireNonNull(this.testPlan);
		int nestingLevel = 0;
		TestIdentifier current = testIdentifier;
		// roots are the only identifiers without a parent in the test plan, so
		// getParent(...) does not fail for anything else
		while (!testPlan.getRoots().contains(current)) {
			Optional<TestIdentifier> parent = testPlan.getParent(current);
			if (parent.isEmpty()) {
				break;
			}
			current = parent.get();
			nestingLevel++;
		}
		return nestingLevel;
	}

	private String verticals(int nestingLevel) {
		return verticals[Math.min(nestingLevel, verticals.length - 1)];
	}

	private void printVerticals(int nestingLevel, String tile) {
		printf(NONE, verticals(nestingLevel));
		printf(NONE, tile);
	}

	private void printf(Style style, String message, Object... args) {
		out.printf(colorPalette.paint(style, message), args);
		out.flush();
	}

	/**
	 * Print single detail with a potential multi-line message.
	 */
	private void printDetail(int nestingLevel, Style style, String detail, String format, Object... args) {
		// print initial verticals - expecting to be at start of the line
		String verticals = verticals(nestingLevel + 1);
		printf(NONE, verticals);
		String detailFormat = "%9s";
		// omit detail string if it's empty
		if (!detail.isEmpty()) {
			printf(NONE, "%s", (detailFormat + ": ").formatted(detail));
		}
		// trivial case: at least one arg is given? Let printf do the entire work
		if (args.length > 0) {
			printf(style, format, args);
			return;
		}
		// still here? Split format into separate lines and indent them from the second line on
		String[] lines = format.split("\\R");
		printf(style, "%s", lines[0]);
		if (lines.length > 1) {
			String delimiter = System.lineSeparator() + verticals + (detailFormat + "    ").formatted("");
			for (int i = 1; i < lines.length; i++) {
				printf(NONE, "%s", delimiter);
				printf(style, "%s", lines[i]);
			}
		}
		printf(NONE, "%n");
	}

	@Override
	public void listTests(TestPlan testPlan) {
		this.testPlan = testPlan;
		testPlan.accept(new TestPlan.Visitor() {
			@Override
			public void preVisitContainer(TestIdentifier testIdentifier) {
				if (!testPlan.getChildren(testIdentifier).isEmpty()) {
					printVerticals(nestingLevel(testIdentifier), theme.entry());
					printf(Style.CONTAINER, " %s", testIdentifier.getDisplayName());
					printf(NONE, "%n");
				}
			}

			@Override
			public void visit(TestIdentifier testIdentifier) {
				if (testPlan.getChildren(testIdentifier).isEmpty()) {
					int nestingLevel = nestingLevel(testIdentifier);
					printVerticals(nestingLevel, theme.entry());
					printf(Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
					printDetails(nestingLevel, testIdentifier);
				}
			}

			@Override
			public void postVisitContainer(TestIdentifier testIdentifier) {
				if (!testPlan.getChildren(testIdentifier).isEmpty()) {
					printVerticals(nestingLevel(testIdentifier), theme.end());
					printf(Style.CONTAINER, " %s%n", testIdentifier.getDisplayName());
				}
			}
		});
	}
}
