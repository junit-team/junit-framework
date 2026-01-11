/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.reporting.legacy.xml;

import static java.util.Collections.emptyList;
import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.4
 */
class XmlReportData {

	private static final int MILLIS_PER_SECOND = 1000;

	private final Map<TestIdentifier, TestExecutionResult> finishedTests = new ConcurrentHashMap<>();
	private final Map<TestIdentifier, String> skippedTests = new ConcurrentHashMap<>();
	private final Map<TestIdentifier, Instant> startInstants = new ConcurrentHashMap<>();
	private final Map<TestIdentifier, Instant> endInstants = new ConcurrentHashMap<>();
	private final Map<TestIdentifier, List<ReportEntry>> reportEntries = new ConcurrentHashMap<>();

	private final TestPlan testPlan;
	private final Clock clock;

	XmlReportData(TestPlan testPlan, Clock clock) {
		this.testPlan = testPlan;
		this.clock = clock;
	}

	TestPlan getTestPlan() {
		return this.testPlan;
	}

	Clock getClock() {
		return this.clock;
	}

	void markSkipped(TestIdentifier testIdentifier, @Nullable String reason) {
		this.skippedTests.put(testIdentifier, reason == null ? "" : reason);
	}

	void markStarted(TestIdentifier testIdentifier) {
		this.startInstants.put(testIdentifier, this.clock.instant());
	}

	void markFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
		this.endInstants.put(testIdentifier, this.clock.instant());
		if (result.getStatus() == ABORTED) {
			String reason = result.getThrowable().map(ExceptionUtils::readStackTrace).orElse("");
			this.skippedTests.put(testIdentifier, reason);
		}
		else {
			this.finishedTests.put(testIdentifier, result);
		}
	}

	void addReportEntry(TestIdentifier testIdentifier, ReportEntry entry) {
		List<ReportEntry> entries = this.reportEntries.computeIfAbsent(testIdentifier, key -> new ArrayList<>());
		entries.add(entry);
	}

	boolean wasSkipped(TestIdentifier testIdentifier) {
		return findSkippedAncestor(testIdentifier).isPresent();
	}

	double getDurationInSeconds(TestIdentifier testIdentifier) {
		Instant startInstant = this.startInstants.getOrDefault(testIdentifier, Instant.EPOCH);
		Instant endInstant = this.endInstants.getOrDefault(testIdentifier, startInstant);
		return Duration.between(startInstant, endInstant).toMillis() / (double) MILLIS_PER_SECOND;
	}

	@Nullable
	String getSkipReason(TestIdentifier testIdentifier) {
		return findSkippedAncestor(testIdentifier).map(skippedTestIdentifier -> {
			String reason = this.skippedTests.get(skippedTestIdentifier);
			if (!testIdentifier.equals(skippedTestIdentifier)) {
				reason = "parent was skipped: " + reason;
			}
			return reason;
		}).orElse(null);
	}

	List<TestExecutionResult> getResults(TestIdentifier testIdentifier) {
		return getAncestors(testIdentifier).stream() //
				.map(this.finishedTests::get) //
				.filter(Objects::nonNull) //
				.toList();
	}

	List<ReportEntry> getReportEntries(TestIdentifier testIdentifier) {
		return this.reportEntries.getOrDefault(testIdentifier, emptyList());
	}

	private Optional<TestIdentifier> findSkippedAncestor(TestIdentifier testIdentifier) {
		return findAncestor(testIdentifier, this.skippedTests::containsKey);
	}

	private Optional<TestIdentifier> findAncestor(TestIdentifier testIdentifier, Predicate<TestIdentifier> predicate) {
		Optional<TestIdentifier> current = Optional.of(testIdentifier);
		while (current.isPresent()) {
			if (predicate.test(current.get())) {
				return current;
			}
			current = this.testPlan.getParent(current.get());
		}
		return Optional.empty();
	}

	private List<TestIdentifier> getAncestors(TestIdentifier testIdentifier) {
		TestIdentifier current = testIdentifier;
		List<TestIdentifier> ancestors = new ArrayList<>();
		while (current != null) {
			ancestors.add(current);
			current = this.testPlan.getParent(current).orElse(null);
		}
		return ancestors;
	}

}
