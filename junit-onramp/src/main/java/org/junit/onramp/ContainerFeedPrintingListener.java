/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.onramp;

import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

class ContainerFeedPrintingListener implements TestExecutionListener {

	private record Summary(int size, int successful, int aborted, int failed, int skipped) {
		Summary with(Summary other) {
			return new Summary(size + other.size, successful + other.successful, aborted + other.aborted,
				failed + other.failed, skipped + other.skipped);
		}
	}

	ContainerFeedPrintingListener() {
	}

	private final Map<String, Summary> summaries = new ConcurrentHashMap<>();

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		printCaptions(); // as header
		summaries.put("/", new Summary(0, 0, 0, 0, 0));
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		if (summaries.size() > 42) {
			printCaptions(); // as footer
		}
	}

	@Override
	public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
		if (identifier.isTest()) {
			var status = result.getStatus();
			var summary = new Summary(1, status == TestExecutionResult.Status.SUCCESSFUL ? 1 : 0,
				status == TestExecutionResult.Status.ABORTED ? 1 : 0,
				status == TestExecutionResult.Status.FAILED ? 1 : 0, 0);
			merge(identifier, summary);
		}
		if (identifier.isContainer()) {
			var summary = summaries.get(identifier.getUniqueId());
			if (summary == null)
				return;
			var indent = " ".repeat(identifier.getUniqueIdObject().getSegments().size());
			var name = identifier.getDisplayName();
			print(summary, indent + name);
		}
	}

	@Override
	public void executionSkipped(TestIdentifier identifier, String reason) {
		if (identifier.isTest()) {
			var summary = new Summary(1, 0, 0, 0, 1);
			merge(identifier, summary);
		}
	}

	private void merge(TestIdentifier identifier, Summary summary) {
		var joiner = new StringJoiner("/");
		var segments = identifier.getUniqueIdObject().getSegments();
		for (var segment : segments) {
			joiner.add('[' + segment.getType() + ':' + segment.getValue() + ']');
			summaries.merge(joiner.toString(), summary, Summary::with);
		}
	}

	private void printCaptions() {
		System.out.printf("%5s %5s %5s %5s %5s %s%n", //
			"Found", "OK", "[A]", "[F]", "[S]", "Display Name");
	}

	private void print(Summary summary, String name) {
		System.out.printf("%5s %5s %5s %5s %5s %s%n", //
			summary.size, summary.successful, summary.aborted, summary.failed, summary.skipped, name);
	}
}
