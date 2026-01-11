/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.descriptor.VintageTestDescriptor;

/**
 * @since 4.12
 */
class UniqueIdFilter extends Filter {

	private final RunnerTestDescriptor runnerTestDescriptor;
	private final UniqueId uniqueId;

	@SuppressWarnings({ "NullAway.Init", "NotNullFieldNotInitialized" })
	private Deque<Description> path;

	@SuppressWarnings({ "NullAway.Init", "NotNullFieldNotInitialized" })
	private Set<Description> descendants;

	UniqueIdFilter(RunnerTestDescriptor runnerTestDescriptor, UniqueId uniqueId) {
		this.runnerTestDescriptor = runnerTestDescriptor;
		this.uniqueId = uniqueId;
	}

	@SuppressWarnings("ConstantValue")
	private void ensureInitialized() {
		if (descendants == null) {
			Optional<? extends TestDescriptor> identifiedTestDescriptor = runnerTestDescriptor.findByUniqueId(uniqueId);
			descendants = determineDescendants(identifiedTestDescriptor);
			path = determinePath(runnerTestDescriptor, identifiedTestDescriptor);
		}
	}

	private Deque<Description> determinePath(RunnerTestDescriptor runnerTestDescriptor,
			Optional<? extends TestDescriptor> identifiedTestDescriptor) {
		Deque<Description> path = new ArrayDeque<>();
		Optional<? extends TestDescriptor> current = identifiedTestDescriptor;
		while (current.isPresent() && !current.get().equals(runnerTestDescriptor)) {
			path.addFirst(((VintageTestDescriptor) current.get()).getDescription());
			current = current.get().getParent();
		}
		return path;
	}

	private Set<Description> determineDescendants(Optional<? extends TestDescriptor> identifiedTestDescriptor) {
		// @formatter:off
		return identifiedTestDescriptor.map(
				testDescriptor -> testDescriptor
						.getDescendants()
						.stream()
						.map(VintageTestDescriptor.class::cast)
						.map(VintageTestDescriptor::getDescription)
						.collect(toSet()))
				.orElseGet(Collections::emptySet);
		// @formatter:on
	}

	@Override
	public boolean shouldRun(Description description) {
		ensureInitialized();
		return path.contains(description) || descendants.contains(description);
	}

	@Override
	public String describe() {
		return "Unique ID " + uniqueId;
	}

}
