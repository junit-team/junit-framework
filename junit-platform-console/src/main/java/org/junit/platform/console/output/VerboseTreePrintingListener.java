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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.ExceptionUtils.readStackTrace;
import static org.junit.platform.console.output.Style.NONE;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;

import org.apiguardian.api.API;
import org.junit.platform.engine.TestExecutionResult;
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
	private final Deque<Long> frames;
	private final String[] verticals;
	private long executionStartedMillis;

	public VerboseTreePrintingListener(PrintWriter out, ColorPalette colorPalette, int maxContainerNestingLevel,
			Theme theme) {
		this.out = out;
		this.colorPalette = colorPalette;
		this.theme = theme;

		// create frame stack and push initial root frame
		this.frames = new ArrayDeque<>();
		this.frames.push(0L);

		// create and populate vertical indentation lookup table
		this.verticals = new String[Math.max(10, maxContainerNestingLevel) + 1];
		this.verticals[0] = ""; // no frame
		this.verticals[1] = ""; // synthetic root "/" level
		this.verticals[2] = ""; // "engine" level

		for (int i = 3; i < verticals.length; i++) {
			verticals[i] = verticals[i - 1] + theme.vertical();
		}
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		frames.push(System.currentTimeMillis());

		String prefix = "Test plan execution started. Number of static tests: ";
		printNumberOfTests(testPlan, prefix);
		printf(Style.CONTAINER, "%s%n", theme.root());
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		frames.pop();

		printNumberOfTests(testPlan, "Test plan execution finished. Number of all tests: ");
	}

	private void printNumberOfTests(TestPlan testPlan, String prefix) {
		long tests = testPlan.countTestIdentifiers(TestIdentifier::isTest);
		printf(NONE, "%s", prefix);
		printf(Style.TEST, "%d%n", tests);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		this.executionStartedMillis = System.currentTimeMillis();
		if (testIdentifier.isContainer()) {
			printVerticals(theme.entry());
			printf(Style.CONTAINER, " %s", testIdentifier.getDisplayName());
			printf(NONE, "%n");
			frames.push(System.currentTimeMillis());
		}
		if (testIdentifier.isContainer()) {
			return;
		}
		printVerticals(theme.entry());
		printf(Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
		printDetails(testIdentifier);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		testExecutionResult.getThrowable().ifPresent(t -> printDetail(Style.FAILED, "caught", readStackTrace(t)));
		if (testIdentifier.isContainer()) {
			Long creationMillis = frames.pop();
			printVerticals(theme.end());
			printf(Style.CONTAINER, " %s", testIdentifier.getDisplayName());
			printf(NONE, " finished after %d ms.%n", System.currentTimeMillis() - creationMillis);
			return;
		}
		printDetail(NONE, "duration", "%d ms%n", System.currentTimeMillis() - executionStartedMillis);
		String status = theme.status(testExecutionResult) + " " + testExecutionResult.getStatus();
		printDetail(Style.valueOf(testExecutionResult), "status", "%s%n", status);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		printVerticals(theme.entry());
		printf(Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
		printDetails(testIdentifier);
		printDetail(Style.SKIPPED, "reason", reason);
		printDetail(Style.SKIPPED, "status", theme.skipped() + " SKIPPED");
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		printVerticals(theme.entry());
		printf(Style.DYNAMIC, " %s", testIdentifier.getDisplayName());
		printf(NONE, "%s%n", " dynamically registered");
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		printDetail(Style.REPORTED, "reports", entry.toString());
	}

	@Override
	public void fileEntryPublished(TestIdentifier testIdentifier, FileEntry file) {
		printDetail(Style.REPORTED, "reports", file.toString());
	}

	/**
	 * Print static information about the test identifier.
	 */
	private void printDetails(TestIdentifier testIdentifier) {
		printDetail(NONE, "tags", "%s%n", testIdentifier.getTags());
		printDetail(NONE, "uniqueId", "%s%n", testIdentifier.getUniqueId());
		printDetail(NONE, "parent", "%s%n", testIdentifier.getParentId().orElse("[]"));
		testIdentifier.getSource().ifPresent(source -> printDetail(NONE, "source", "%s%n", source));
	}

	private String verticals() {
		return verticals(frames.size());
	}

	private String verticals(int index) {
		return verticals[Math.min(index, verticals.length - 1)];
	}

	private void printVerticals(String tile) {
		printf(NONE, verticals());
		printf(NONE, tile);
	}

	private void printf(Style style, String message, Object... args) {
		out.printf(colorPalette.paint(style, message), args);
		out.flush();
	}

	/**
	 * Print single detail with a potential multi-line message.
	 */
	private void printDetail(Style style, String detail, String format, Object... args) {
		// print initial verticals - expecting to be at start of the line
		String verticals = verticals(frames.size() + 1);
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
		frames.push(0L);
		testPlan.accept(new TestPlan.Visitor() {
			@Override
			public void preVisitContainer(TestIdentifier testIdentifier) {
				if (!testPlan.getChildren(testIdentifier).isEmpty()) {
					printVerticals(theme.entry());
					printf(Style.CONTAINER, " %s", testIdentifier.getDisplayName());
					printf(NONE, "%n");
					frames.push(0L);
				}
			}

			@Override
			public void visit(TestIdentifier testIdentifier) {
				if (testPlan.getChildren(testIdentifier).isEmpty()) {
					printVerticals(theme.entry());
					printf(Style.valueOf(testIdentifier), " %s%n", testIdentifier.getDisplayName());
					printDetails(testIdentifier);
				}
			}

			@Override
			public void postVisitContainer(TestIdentifier testIdentifier) {
				if (!testPlan.getChildren(testIdentifier).isEmpty()) {
					frames.pop();
					printVerticals(theme.end());
					printf(Style.CONTAINER, " %s%n", testIdentifier.getDisplayName());
				}
			}
		});
		frames.pop();
	}
}
