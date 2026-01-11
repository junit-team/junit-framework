/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.platform.engine.FilterResult.includedIf;

import java.util.Arrays;
import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestEngine;

/**
 * An {@code EngineFilter} is applied to all {@link TestEngine TestEngines}
 * before they are used.
 *
 * <p><strong>Warning</strong>: be cautious when registering multiple competing
 * {@link #includeEngines include} {@code EngineFilters} or multiple competing
 * {@link #excludeEngines exclude} {@code EngineFilters} for the same discovery
 * request since doing so will likely lead to undesirable results (i.e., zero
 * engines being active).
 *
 * @since 1.0
 * @see #includeEngines(String...)
 * @see #excludeEngines(String...)
 * @see LauncherDiscoveryRequest
 */
@API(status = STABLE, since = "1.0")
public class EngineFilter implements Filter<TestEngine> {

	/**
	 * Create a new <em>include</em> {@code EngineFilter} based on the
	 * supplied engine IDs.
	 *
	 * <p>Only {@code TestEngines} with matching engine IDs will be
	 * <em>included</em> within the test discovery and execution.
	 *
	 * @param engineIds the list of engine IDs to match against; never {@code null}
	 * or empty; individual IDs must also not be null or blank
	 * @see #includeEngines(String...)
	 */
	public static EngineFilter includeEngines(String... engineIds) {
		return includeEngines(Arrays.asList(engineIds));
	}

	/**
	 * Create a new <em>include</em> {@code EngineFilter} based on the
	 * supplied engine IDs.
	 *
	 * <p>Only {@code TestEngines} with matching engine IDs will be
	 * <em>included</em> within the test discovery and execution.
	 *
	 * @param engineIds the list of engine IDs to match against; never {@code null}
	 * or empty; individual IDs must also not be null or blank
	 * @see #includeEngines(String...)
	 */
	public static EngineFilter includeEngines(List<String> engineIds) {
		return new EngineFilter(engineIds, Type.INCLUDE);
	}

	/**
	 * Create a new <em>exclude</em> {@code EngineFilter} based on the
	 * supplied engine IDs.
	 *
	 * <p>{@code TestEngines} with matching engine IDs will be
	 * <em>excluded</em> from test discovery and execution.
	 *
	 * @param engineIds the list of engine IDs to match against; never {@code null}
	 * or empty; individual IDs must also not be null or blank
	 * @see #excludeEngines(List)
	 */
	public static EngineFilter excludeEngines(String... engineIds) {
		return excludeEngines(Arrays.asList(engineIds));
	}

	/**
	 * Create a new <em>exclude</em> {@code EngineFilter} based on the
	 * supplied engine IDs.
	 *
	 * <p>{@code TestEngines} with matching engine IDs will be
	 * <em>excluded</em> from test discovery and execution.
	 *
	 * @param engineIds the list of engine IDs to match against; never {@code null}
	 * or empty; individual IDs must also not be null or blank
	 * @see #includeEngines(String...)
	 */
	public static EngineFilter excludeEngines(List<String> engineIds) {
		return new EngineFilter(engineIds, Type.EXCLUDE);
	}

	private final List<String> engineIds;
	private final Type type;

	private EngineFilter(List<String> engineIds, Type type) {
		this.engineIds = validateAndTrim(engineIds);
		this.type = type;
	}

	@API(status = INTERNAL, since = "1.9")
	public List<String> getEngineIds() {
		return engineIds;
	}

	@API(status = INTERNAL, since = "1.9")
	public boolean isIncludeFilter() {
		return type == Type.INCLUDE;
	}

	@Override
	public FilterResult apply(TestEngine testEngine) {
		Preconditions.notNull(testEngine, "TestEngine must not be null");
		String engineId = testEngine.getId();
		Preconditions.notBlank(engineId, "TestEngine ID must not be null or blank");

		if (this.type == Type.INCLUDE) {
			return includedIf(this.engineIds.stream().anyMatch(engineId::equals), //
				() -> "Engine ID [%s] is in included list [%s]".formatted(engineId, this.engineIds), //
				() -> "Engine ID [%s] is not in included list [%s]".formatted(engineId, this.engineIds));
		}
		else {
			return includedIf(this.engineIds.stream().noneMatch(engineId::equals), //
				() -> "Engine ID [%s] is not in excluded list [%s]".formatted(engineId, this.engineIds), //
				() -> "Engine ID [%s] is in excluded list [%s]".formatted(engineId, this.engineIds));
		}
	}

	@Override
	public String toString() {
		return "%s that %s engines with IDs %s".formatted(getClass().getSimpleName(), this.type.verb, this.engineIds);
	}

	private static List<String> validateAndTrim(List<String> engineIds) {
		Preconditions.notEmpty(engineIds, "engine ID list must not be null or empty");

		// @formatter:off
		return engineIds.stream()
				.map(id -> Preconditions.notBlank(id, "engine ID must not be null or blank").strip())
				.distinct()
				.toList();
		// @formatter:on
	}

	private enum Type {

		INCLUDE("includes"),

		EXCLUDE("excludes");

		private final String verb;

		Type(String verb) {
			this.verb = verb;
		}

	}

}
