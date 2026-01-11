/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.jfr;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.FileEntry;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * A {@link TestExecutionListener} that generates Java Flight Recorder
 * events.
 *
 * @since 1.8
 * @see <a href="https://openjdk.java.net/jeps/328">JEP 328: Flight Recorder</a>
 */
@API(status = INTERNAL, since = "6.0")
class FlightRecordingExecutionListener implements TestExecutionListener {

	private final Map<org.junit.platform.engine.UniqueId, TestExecutionEvent> testExecutionEvents = new ConcurrentHashMap<>();
	private @Nullable TestPlanExecutionEvent testPlanExecutionEvent;

	@Override
	public void testPlanExecutionStarted(TestPlan plan) {
		var event = new TestPlanExecutionEvent();
		if (event.isEnabled()) {
			event.begin();
			this.testPlanExecutionEvent = event;
		}
	}

	@Override
	public void testPlanExecutionFinished(TestPlan plan) {
		var event = this.testPlanExecutionEvent;
		this.testPlanExecutionEvent = null;
		if (event != null && event.shouldCommit()) {
			event.containsTests = plan.containsTests();
			event.engineNames = plan.getRoots().stream().map(TestIdentifier::getDisplayName).collect(
				Collectors.joining(", "));
			event.commit();
		}
	}

	@Override
	public void executionSkipped(TestIdentifier test, String reason) {
		var event = new SkippedTestEvent();
		if (event.shouldCommit()) {
			event.initialize(test);
			event.reason = reason;
			event.commit();
		}
	}

	@Override
	public void executionStarted(TestIdentifier test) {
		var event = new TestExecutionEvent();
		if (event.isEnabled()) {
			event.begin();
			this.testExecutionEvents.put(test.getUniqueIdObject(), event);
		}
	}

	@Override
	public void executionFinished(TestIdentifier test, TestExecutionResult result) {
		TestExecutionEvent event = this.testExecutionEvents.remove(test.getUniqueIdObject());
		if (event != null && event.shouldCommit()) {
			event.end();
			event.initialize(test);
			event.result = result.getStatus().toString();
			Optional<Throwable> throwable = result.getThrowable();
			event.exceptionClass = throwable.map(Throwable::getClass).orElse(null);
			event.exceptionMessage = throwable.map(Throwable::getMessage).orElse(null);
			event.commit();
		}
	}

	@Override
	public void reportingEntryPublished(TestIdentifier test, ReportEntry reportEntry) {
		for (var entry : reportEntry.getKeyValuePairs().entrySet()) {
			var event = new ReportEntryEvent();
			if (event.shouldCommit()) {
				event.uniqueId = test.getUniqueId();
				event.key = entry.getKey();
				event.value = entry.getValue();
				event.commit();
			}
		}
	}

	@Override
	public void fileEntryPublished(TestIdentifier testIdentifier, FileEntry file) {
		var event = new FileEntryEvent();
		if (event.shouldCommit()) {
			event.uniqueId = testIdentifier.getUniqueId();
			event.path = file.getPath().toAbsolutePath().toString();
			event.commit();
		}
	}

	@Category({ "JUnit", "Execution" })
	@StackTrace(false)
	abstract static class ExecutionEvent extends Event {
	}

	@Label("Test Execution")
	@Name("org.junit.TestPlanExecution")
	static class TestPlanExecutionEvent extends ExecutionEvent {

		@Label("Contains Tests")
		boolean containsTests;

		@Label("Engine Names")
		@Nullable
		String engineNames;
	}

	abstract static class TestEvent extends ExecutionEvent {

		@UniqueId
		@Label("Unique Id")
		@Nullable
		String uniqueId;

		@Label("Display Name")
		@Nullable
		String displayName;

		@Label("Tags")
		@Nullable
		String tags;

		@Label("Type")
		@Nullable
		String type;

		void initialize(TestIdentifier test) {
			this.uniqueId = test.getUniqueId();
			this.displayName = test.getDisplayName();
			this.tags = test.getTags().isEmpty() ? null : test.getTags().toString();
			this.type = test.getType().name();
		}
	}

	@Label("Skipped Test")
	@Name("org.junit.SkippedTest")
	static class SkippedTestEvent extends TestEvent {
		@Label("Reason")
		@Nullable
		String reason;
	}

	@Label("Test")
	@Name("org.junit.TestExecution")
	static class TestExecutionEvent extends TestEvent {

		@Label("Result")
		@Nullable
		String result;

		@Label("Exception Class")
		@Nullable
		Class<?> exceptionClass;

		@Label("Exception Message")
		@Nullable
		String exceptionMessage;
	}

	@Label("Report Entry")
	@Name("org.junit.ReportEntry")
	static class ReportEntryEvent extends ExecutionEvent {

		@UniqueId
		@Label("Unique Id")
		@Nullable
		String uniqueId;

		@Label("Key")
		@Nullable
		String key;

		@Label("Value")
		@Nullable
		String value;
	}

	@Label("File Entry")
	@Name("org.junit.FileEntry")
	static class FileEntryEvent extends ExecutionEvent {

		@UniqueId
		@Label("Unique Id")
		@Nullable
		String uniqueId;

		@Label("Path")
		@Nullable
		String path;
	}
}
