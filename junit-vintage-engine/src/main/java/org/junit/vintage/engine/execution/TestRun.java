/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.execution;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.runner.Description;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.descriptor.VintageTestDescriptor;
import org.opentest4j.MultipleFailuresError;

/**
 * @since 4.12
 */
class TestRun {

	private final RunnerTestDescriptor runnerTestDescriptor;
	private final Set<TestDescriptor> runnerDescendants;
	private final Map<Description, VintageDescriptors> descriptionToDescriptors;
	private final Map<TestDescriptor, List<TestExecutionResult>> executionResults = new LinkedHashMap<>();
	private final Set<TestDescriptor> skippedDescriptors = new LinkedHashSet<>();
	private final Set<TestDescriptor> startedDescriptors = new HashSet<>();
	private final Map<TestDescriptor, EventType> inProgressDescriptors = new LinkedHashMap<>();
	private final Set<TestDescriptor> finishedDescriptors = new LinkedHashSet<>();
	private final ThreadLocal<Deque<VintageTestDescriptor>> inProgressDescriptorsByStartingThread = ThreadLocal.withInitial(
		ArrayDeque::new);

	TestRun(RunnerTestDescriptor runnerTestDescriptor) {
		this.runnerTestDescriptor = runnerTestDescriptor;
		runnerDescendants = new LinkedHashSet<>(runnerTestDescriptor.getDescendants());
		// @formatter:off
		descriptionToDescriptors = concat(Stream.of(runnerTestDescriptor), runnerDescendants.stream())
				.map(VintageTestDescriptor.class::cast)
				.collect(toMap(VintageTestDescriptor::getDescription, VintageDescriptors::new, VintageDescriptors::merge, HashMap::new));
		// @formatter:on
	}

	void registerDynamicTest(VintageTestDescriptor testDescriptor) {
		descriptionToDescriptors.computeIfAbsent(testDescriptor.getDescription(), __ -> new VintageDescriptors()).add(
			testDescriptor);
		runnerDescendants.add(testDescriptor);
	}

	RunnerTestDescriptor getRunnerTestDescriptor() {
		return runnerTestDescriptor;
	}

	Collection<TestDescriptor> getInProgressTestDescriptorsWithSyntheticStartEvents() {
		List<TestDescriptor> result = inProgressDescriptors.entrySet().stream() //
				.filter(entry -> entry.getValue().equals(EventType.SYNTHETIC)) //
				.map(Entry::getKey) //
				.collect(toCollection(ArrayList::new));
		Collections.reverse(result);
		return result;
	}

	Collection<TestDescriptor> getInProgressTestDescriptors() {
		List<TestDescriptor> result = new ArrayList<>(inProgressDescriptors.keySet());
		Collections.reverse(result);
		return result;
	}

	boolean isDescendantOfRunnerTestDescriptor(TestDescriptor testDescriptor) {
		return runnerDescendants.contains(testDescriptor);
	}

	boolean hasSyntheticStartEvent(TestDescriptor testDescriptor) {
		return inProgressDescriptors.get(testDescriptor) == EventType.SYNTHETIC;
	}

	Optional<VintageTestDescriptor> lookupNextTestDescriptor(Description description) {
		return lookupUnambiguouslyOrApplyFallback(description, VintageDescriptors::getNextUnstarted);
	}

	Optional<VintageTestDescriptor> lookupCurrentTestDescriptor(Description description) {
		return lookupUnambiguouslyOrApplyFallback(description, __ -> {
			VintageTestDescriptor lastStarted = inProgressDescriptorsByStartingThread.get().peekLast();
			if (lastStarted != null && description.equals(lastStarted.getDescription())) {
				return Optional.of(lastStarted);
			}
			return Optional.empty();
		});
	}

	private Optional<VintageTestDescriptor> lookupUnambiguouslyOrApplyFallback(Description description,
			Function<VintageDescriptors, Optional<VintageTestDescriptor>> fallback) {
		VintageDescriptors vintageDescriptors = descriptionToDescriptors.getOrDefault(description,
			VintageDescriptors.NONE);
		Optional<VintageTestDescriptor> result = vintageDescriptors.getUnambiguously(description);
		if (result.isEmpty()) {
			result = fallback.apply(vintageDescriptors);
		}
		return result;
	}

	void markSkipped(TestDescriptor testDescriptor) {
		skippedDescriptors.add(testDescriptor);
		if (testDescriptor instanceof VintageTestDescriptor vintageDescriptor) {
			getVintageDescriptors(vintageDescriptor).incrementSkippedOrStarted();
		}
	}

	boolean isNotSkipped(TestDescriptor testDescriptor) {
		return !isSkipped(testDescriptor);
	}

	boolean isSkipped(TestDescriptor testDescriptor) {
		return skippedDescriptors.contains(testDescriptor);
	}

	void markStarted(TestDescriptor testDescriptor, EventType eventType) {
		inProgressDescriptors.put(testDescriptor, eventType);
		startedDescriptors.add(testDescriptor);
		if (testDescriptor instanceof VintageTestDescriptor vintageDescriptor) {
			inProgressDescriptorsByStartingThread.get().addLast(vintageDescriptor);
			getVintageDescriptors(vintageDescriptor).incrementSkippedOrStarted();
		}
	}

	private VintageDescriptors getVintageDescriptors(VintageTestDescriptor vintageDescriptor) {
		return requireNonNull(descriptionToDescriptors.get(vintageDescriptor.getDescription()),
			() -> "No descriptors for " + vintageDescriptor);
	}

	boolean isNotStarted(TestDescriptor testDescriptor) {
		return !startedDescriptors.contains(testDescriptor);
	}

	void markFinished(TestDescriptor testDescriptor) {
		inProgressDescriptors.remove(testDescriptor);
		finishedDescriptors.add(testDescriptor);
		if (testDescriptor instanceof VintageTestDescriptor descriptor) {
			inProgressDescriptorsByStartingThread.get().removeLastOccurrence(descriptor);
		}
	}

	boolean isNotFinished(TestDescriptor testDescriptor) {
		return !isFinished(testDescriptor);
	}

	boolean isFinished(TestDescriptor testDescriptor) {
		return finishedDescriptors.contains(testDescriptor);
	}

	boolean areAllFinishedOrSkipped(Set<? extends TestDescriptor> testDescriptors) {
		return testDescriptors.stream().allMatch(this::isFinishedOrSkipped);
	}

	boolean isFinishedOrSkipped(TestDescriptor testDescriptor) {
		return isFinished(testDescriptor) || isSkipped(testDescriptor);
	}

	void storeResult(TestDescriptor testDescriptor, TestExecutionResult result) {
		List<TestExecutionResult> testExecutionResults = executionResults.computeIfAbsent(testDescriptor,
			key -> new ArrayList<>());
		testExecutionResults.add(result);
	}

	TestExecutionResult getStoredResultOrSuccessful(TestDescriptor testDescriptor) {
		List<TestExecutionResult> testExecutionResults = executionResults.get(testDescriptor);

		if (testExecutionResults == null) {
			return successful();
		}
		if (testExecutionResults.size() == 1) {
			return testExecutionResults.get(0);
		}
		// @formatter:off
		List<Throwable> failures = testExecutionResults
				.stream()
				.map(TestExecutionResult::getThrowable)
				.map(Optional::orElseThrow)
				.toList();
		// @formatter:on
		MultipleFailuresError multipleFailuresError = new MultipleFailuresError("", failures);
		failures.forEach(multipleFailuresError::addSuppressed);
		return failed(multipleFailuresError);
	}

	private static class VintageDescriptors {

		private static final VintageDescriptors NONE = new VintageDescriptors(emptyList());

		private final List<VintageTestDescriptor> descriptors;
		private int skippedOrStartedCount;

		static VintageDescriptors merge(VintageDescriptors a, VintageDescriptors b) {
			List<VintageTestDescriptor> mergedDescriptors = new ArrayList<>(
				a.descriptors.size() + b.descriptors.size());
			mergedDescriptors.addAll(a.descriptors);
			mergedDescriptors.addAll(b.descriptors);
			return new VintageDescriptors(mergedDescriptors);
		}

		VintageDescriptors(VintageTestDescriptor vintageTestDescriptor) {
			this();
			add(vintageTestDescriptor);
		}

		VintageDescriptors() {
			this(new ArrayList<>(1));
		}

		VintageDescriptors(List<VintageTestDescriptor> descriptors) {
			this.descriptors = descriptors;
		}

		void add(VintageTestDescriptor descriptor) {
			descriptors.add(descriptor);
		}

		/**
		 * Returns the {@link TestDescriptor} that represents the specified
		 * {@link Description}.
		 *
		 * <p>There are edge cases where multiple {@link Description Descriptions}
		 * with the same {@code uniqueId} exist, e.g. when using overloaded methods
		 * to define {@linkplain org.junit.experimental.theories.Theory theories}.
		 * In this case, we try to find the correct {@link TestDescriptor} by
		 * checking for object identity on the {@link Description} it represents.
		 *
		 * @param description the {@code Description} to look up
		 */
		@SuppressWarnings("ReferenceEquality")
		Optional<VintageTestDescriptor> getUnambiguously(Description description) {
			if (descriptors.isEmpty()) {
				return Optional.empty();
			}
			if (descriptors.size() == 1) {
				return Optional.of(descriptors.get(0));
			}
			// @formatter:off
			return descriptors.stream()
					.filter(testDescriptor -> description == testDescriptor.getDescription())
					.findFirst();
			// @formatter:on
		}

		private void incrementSkippedOrStarted() {
			skippedOrStartedCount++;
		}

		private Optional<VintageTestDescriptor> getNextUnstarted() {
			if (skippedOrStartedCount < descriptors.size()) {
				return Optional.of(descriptors.get(skippedOrStartedCount));
			}
			return Optional.empty();
		}

	}

}
