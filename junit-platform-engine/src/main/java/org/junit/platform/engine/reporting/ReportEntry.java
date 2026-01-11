/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.reporting;

import static org.apiguardian.api.API.Status.STABLE;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * {@code ReportEntry} encapsulates a time-stamped map of {@code String}-based
 * key-value pairs to be published to the reporting infrastructure.
 *
 * @since 1.0
 * @see #from(Map)
 * @see #from(String, String)
 */
@API(status = STABLE, since = "1.0")
public final class ReportEntry {

	private final LocalDateTime timestamp = LocalDateTime.now(ZoneId.systemDefault());
	private final Map<String, String> keyValuePairs = new LinkedHashMap<>();

	private ReportEntry() {
	}

	/**
	 * Factory for creating a new {@code ReportEntry} from a map of key-value pairs.
	 *
	 * @param keyValuePairs the map of key-value pairs to be published; never
	 * {@code null}; keys and values within entries in the map also must not be
	 * {@code null} or blank
	 */
	public static ReportEntry from(Map<String, String> keyValuePairs) {
		Preconditions.notNull(keyValuePairs, "keyValuePairs must not be null");

		ReportEntry reportEntry = new ReportEntry();
		keyValuePairs.forEach(reportEntry::add);
		return reportEntry;
	}

	/**
	 * Factory for creating a new {@code ReportEntry} from a key-value pair.
	 *
	 * @param key the key under which the value should published; never
	 * {@code null} or blank
	 * @param value the value to publish; never {@code null} or blank
	 */
	public static ReportEntry from(String key, String value) {
		ReportEntry reportEntry = new ReportEntry();
		reportEntry.add(key, value);
		return reportEntry;
	}

	private void add(String key, String value) {
		Preconditions.notBlank(key, "key must not be null or blank");
		Preconditions.notBlank(value, "value must not be null or blank");
		this.keyValuePairs.put(key, value);
	}

	/**
	 * Get an unmodifiable copy of the map of key-value pairs to be published.
	 *
	 * @return a copy of the map of key-value pairs; never {@code null}
	 */
	public Map<String, String> getKeyValuePairs() {
		return Collections.unmodifiableMap(this.keyValuePairs);
	}

	/**
	 * Get the timestamp for when this {@code ReportEntry} was created.
	 *
	 * <p>Can be used, for example, to order entries.
	 *
	 * @return when this entry was created; never {@code null}
	 */
	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this);
		builder.append("timestamp", this.timestamp);
		this.keyValuePairs.forEach(builder::append);
		return builder.toString();
	}

}
