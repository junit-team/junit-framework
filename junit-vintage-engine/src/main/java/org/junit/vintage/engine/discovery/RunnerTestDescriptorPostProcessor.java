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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import org.junit.platform.engine.UniqueId;
import org.junit.runner.Description;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.descriptor.TestSourceProvider;
import org.junit.vintage.engine.descriptor.VintageTestDescriptor;
import org.junit.vintage.engine.support.UniqueIdReader;
import org.junit.vintage.engine.support.UniqueIdStringifier;

/**
 * @since 5.5
 */
class RunnerTestDescriptorPostProcessor {

	private final UniqueIdReader uniqueIdReader = new UniqueIdReader();
	private final UniqueIdStringifier uniqueIdStringifier = new UniqueIdStringifier();
	private final TestSourceProvider testSourceProvider = new TestSourceProvider();

	void applyFiltersAndCreateDescendants(RunnerTestDescriptor runnerTestDescriptor) {
		addChildrenRecursively(runnerTestDescriptor);
		runnerTestDescriptor.applyFilters(this::addChildrenRecursively);
	}

	private void addChildrenRecursively(VintageTestDescriptor parent) {
		if (parent.getDescription().isTest()) {
			return;
		}
		List<Description> children = parent.getDescription().getChildren();
		// Use LinkedHashMap to preserve order, ArrayList for fast access by index
		Map<String, List<Description>> childrenByUniqueId = children.stream().collect(
			groupingBy(uniqueIdReader.andThen(uniqueIdStringifier), LinkedHashMap::new, toCollection(ArrayList::new)));
		for (Entry<String, List<Description>> entry : childrenByUniqueId.entrySet()) {
			String uniqueId = entry.getKey();
			List<Description> childrenWithSameUniqueId = entry.getValue();
			IntFunction<String> uniqueIdGenerator = determineUniqueIdGenerator(uniqueId, childrenWithSameUniqueId);
			for (int index = 0; index < childrenWithSameUniqueId.size(); index++) {
				String reallyUniqueId = uniqueIdGenerator.apply(index);
				Description description = childrenWithSameUniqueId.get(index);
				UniqueId id = parent.getUniqueId().append(VintageTestDescriptor.SEGMENT_TYPE_TEST, reallyUniqueId);
				VintageTestDescriptor child = new VintageTestDescriptor(id, description,
					testSourceProvider.findTestSource(description));
				parent.addChild(child);
				addChildrenRecursively(child);
			}
		}
	}

	private IntFunction<String> determineUniqueIdGenerator(String uniqueId,
			List<Description> childrenWithSameUniqueId) {
		if (childrenWithSameUniqueId.size() == 1) {
			return index -> uniqueId;
		}
		return index -> uniqueId + "[" + index + "]";
	}
}
