/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.EngineFilter;

class EngineFilterer {

	private static final Logger LOGGER = LoggerFactory.getLogger(EngineFilterer.class);

	private final List<EngineFilter> engineFilters;

	private final Map<TestEngine, Boolean> checkedTestEngines = new HashMap<>();

	EngineFilterer(List<EngineFilter> engineFilters) {
		this.engineFilters = engineFilters;
	}

	boolean isExcluded(TestEngine testEngine) {
		boolean excluded = engineFilters.stream() //
				.map(engineFilter -> engineFilter.apply(testEngine)) //
				.anyMatch(FilterResult::excluded);
		checkedTestEngines.put(testEngine, excluded);
		return excluded;
	}

	void performSanityChecks() {
		checkNoUnmatchedIncludeFilter();
		warnIfAllEnginesExcluded();
	}

	private void warnIfAllEnginesExcluded() {
		if (!checkedTestEngines.isEmpty() && checkedTestEngines.values().stream().allMatch(excluded -> excluded)) {
			LOGGER.warn(() -> "All TestEngines were excluded by EngineFilters.\n" + getStateDescription());
		}
	}

	private void checkNoUnmatchedIncludeFilter() {
		SortedSet<String> unmatchedEngineIdsOfIncludeFilters = getUnmatchedEngineIdsOfIncludeFilters();
		if (!unmatchedEngineIdsOfIncludeFilters.isEmpty()) {
			throw new JUnitException("No TestEngine ID matched the following include EngineFilters: "
					+ unmatchedEngineIdsOfIncludeFilters + ".\n" //
					+ "Please fix/remove the filter or add the engine.\n" //
					+ getStateDescription());
		}
	}

	private SortedSet<String> getUnmatchedEngineIdsOfIncludeFilters() {
		Set<String> checkedTestEngineIds = checkedTestEngines.keySet().stream() //
				.map(TestEngine::getId) //
				.collect(toSet());
		return engineFilters.stream() //
				.filter(EngineFilter::isIncludeFilter) //
				.map(EngineFilter::getEngineIds) //
				.flatMap(Collection::stream) //
				.filter(id -> !checkedTestEngineIds.contains(id)) //
				.collect(toCollection(TreeSet::new));
	}

	private String getStateDescription() {
		return getRegisteredEnginesDescription() + "\n" + getRegisteredFiltersDescription();
	}

	private String getRegisteredEnginesDescription() {
		return TestEngineFormatter.format("Registered TestEngines", checkedTestEngines.keySet());
	}

	private String getRegisteredFiltersDescription() {
		return "Registered EngineFilters:" + engineFilters.stream() //
				.map(Objects::toString) //
				.collect(joining("\n- ", "\n- ", ""));
	}
}
