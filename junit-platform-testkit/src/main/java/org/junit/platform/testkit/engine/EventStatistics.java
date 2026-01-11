/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.junit.platform.testkit.engine.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apiguardian.api.API;
import org.junit.platform.testkit.engine.Assertions.Executable;

/**
 * {@code EventStatistics} provides a fluent API for asserting statistics
 * for {@linkplain Event events}.
 *
 * <p>{@code EventStatistics} is used in conjunction with
 * {@link Events#assertStatistics(java.util.function.Consumer)} as in the
 * following example.
 *
 * <p>{@code events.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));}
 *
 * @since 1.4
 * @see Event
 * @see Events
 */
@API(status = MAINTAINED, since = "1.7")
public class EventStatistics {

	private final List<Executable> executables = new ArrayList<>();
	private final Events events;

	EventStatistics(Events events, String category) {
		this.events = events;
	}

	void assertAll() {
		Assertions.assertAll(this.events.getCategory() + " Event Statistics", this.executables.stream());
	}

	// -------------------------------------------------------------------------

	/**
	 * Specify the number of expected <em>skipped</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 */
	public EventStatistics skipped(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.skipped().count(), "skipped"));
		return this;
	}

	/**
	 * Specify the number of expected <em>started</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 */
	public EventStatistics started(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.started().count(), "started"));
		return this;
	}

	/**
	 * Specify the number of expected <em>finished</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 */
	public EventStatistics finished(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.finished().count(), "finished"));
		return this;
	}

	/**
	 * Specify the number of expected <em>aborted</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 */
	public EventStatistics aborted(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.aborted().count(), "aborted"));
		return this;
	}

	/**
	 * Specify the number of expected <em>succeeded</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 */
	public EventStatistics succeeded(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.succeeded().count(), "succeeded"));
		return this;
	}

	/**
	 * Specify the number of expected <em>failed</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 */
	public EventStatistics failed(long expected) {
		this.executables.add(() -> assertEquals(expected, this.events.failed().count(), "failed"));
		return this;
	}

	/**
	 * Specify the number of expected <em>reporting entry publication</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 */
	public EventStatistics reportingEntryPublished(long expected) {
		this.executables.add(
			() -> assertEquals(expected, this.events.reportingEntryPublished().count(), "reporting entry published"));
		return this;
	}

	/**
	 * Specify the number of expected <em>file entry publication</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 * @since 1.12
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public EventStatistics fileEntryPublished(long expected) {
		this.executables.add(
			() -> assertEquals(expected, this.events.fileEntryPublished().count(), "file entry published"));
		return this;
	}

	/**
	 * Specify the number of expected <em>dynamic registration</em> events.
	 *
	 * @param expected the expected number of events
	 * @return this {@code EventStatistics} for method chaining
	 */
	public EventStatistics dynamicallyRegistered(long expected) {
		this.executables.add(
			() -> assertEquals(expected, this.events.dynamicallyRegistered().count(), "dynamically registered"));
		return this;
	}

}
