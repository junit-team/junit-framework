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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.assertj.core.api.ListAssert;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;

/**
 * {@code Executions} is a facade that provides a fluent API for working with
 * {@linkplain Execution executions}.
 *
 * @since 1.4
 */
@API(status = MAINTAINED, since = "1.7")
public final class Executions {

	private final List<Execution> executions;
	private final String category;

	private Executions(Stream<Execution> executions, String category) {
		Preconditions.notNull(executions, "Execution stream must not be null");

		this.executions = executions.toList();
		this.category = category;
	}

	Executions(List<Event> events, String category) {
		Preconditions.notNull(events, "Event list must not be null");
		Preconditions.containsNoNullElements(events, "Event list must not contain null elements");

		this.executions = List.copyOf(createExecutions(events));
		this.category = category;
	}

	// --- Accessors -----------------------------------------------------------

	/**
	 * Get the {@linkplain Execution executions} as a {@link List}.
	 *
	 * @return the list of executions; never {@code null}
	 * @see #stream()
	 */
	public List<Execution> list() {
		return this.executions;
	}

	/**
	 * Get the {@linkplain Execution executions} as a {@link Stream}.
	 *
	 * @return the stream of executions; never {@code null}
	 * @see #list()
	 */
	public Stream<Execution> stream() {
		return this.executions.stream();
	}

	/**
	 * Shortcut for {@code executions.stream().map(mapper)}.
	 *
	 * @param mapper a {@code Function} to apply to each execution;
	 * never {@code null}
	 * @return the mapped stream of executions; never {@code null}
	 * @see #stream()
	 * @see Stream#map(Function)
	 */
	public <R> Stream<R> map(Function<? super Execution, ? extends R> mapper) {
		Preconditions.notNull(mapper, "Mapping function must not be null");
		return stream().map(mapper);
	}

	/**
	 * Shortcut for {@code executions.stream().filter(predicate)}.
	 *
	 * @param predicate a {@code Predicate} to apply to each execution to decide
	 * if it should be included in the filtered stream; never {@code null}
	 * @return the filtered stream of executions; never {@code null}
	 * @see #stream()
	 * @see Stream#filter(Predicate)
	 */
	public Stream<Execution> filter(Predicate<? super Execution> predicate) {
		Preconditions.notNull(predicate, "Filter predicate must not be null");
		return stream().filter(predicate);
	}

	// --- Statistics ----------------------------------------------------------

	/**
	 * Get the number of {@linkplain Execution executions} contained in this
	 * {@code Executions} object.
	 */
	public long count() {
		return this.executions.size();
	}

	// --- Built-in Filters ----------------------------------------------------

	/**
	 * Get the skipped {@link Executions} contained in this {@code Executions} object.
	 *
	 * @return the filtered {@code Executions}; never {@code null}
	 */
	public Executions skipped() {
		return new Executions(executionsByTerminationInfo(TerminationInfo::skipped), this.category + " Skipped");
	}

	/**
	 * Get the started {@link Executions} contained in this {@code Executions} object.
	 *
	 * @return the filtered {@code Executions}; never {@code null}
	 */
	public Executions started() {
		return new Executions(executionsByTerminationInfo(TerminationInfo::notSkipped), this.category + " Started");
	}

	/**
	 * Get the finished {@link Executions} contained in this {@code Executions} object.
	 *
	 * @return the filtered {@code Executions}; never {@code null}
	 */
	public Executions finished() {
		return new Executions(finishedExecutions(), this.category + " Finished");
	}

	/**
	 * Get the aborted {@link Executions} contained in this {@code Executions} object.
	 *
	 * @return the filtered {@code Executions}; never {@code null}
	 */
	public Executions aborted() {
		return new Executions(finishedExecutionsByStatus(Status.ABORTED), this.category + " Aborted");
	}

	/**
	 * Get the succeeded {@link Executions} contained in this {@code Executions} object.
	 *
	 * @return the filtered {@code Executions}; never {@code null}
	 */
	public Executions succeeded() {
		return new Executions(finishedExecutionsByStatus(Status.SUCCESSFUL), this.category + " Successful");
	}

	/**
	 * Get the failed {@link Executions} contained in this {@code Executions} object.
	 *
	 * @return the filtered {@code Executions}; never {@code null}
	 */
	public Executions failed() {
		return new Executions(finishedExecutionsByStatus(Status.FAILED), this.category + " Failed");
	}

	// --- Assertions ----------------------------------------------------------

	/**
	 * Shortcut for {@code org.assertj.core.api.Assertions.assertThat(executions.list())}.
	 *
	 * @return an instance of {@link ListAssert} for executions; never {@code null}
	 * @see org.assertj.core.api.Assertions#assertThat(List)
	 * @see org.assertj.core.api.ListAssert
	 */
	public ListAssert<Execution> assertThatExecutions() {
		return org.assertj.core.api.Assertions.assertThat(list());
	}

	// --- Diagnostics ---------------------------------------------------------

	/**
	 * Print all executions to {@link System#out}.
	 *
	 * @return this {@code Executions} object for method chaining; never {@code null}
	 */
	public Executions debug() {
		debug(System.out);
		return this;
	}

	/**
	 * Print all executions to the supplied {@link OutputStream}.
	 *
	 * @param out the {@code OutputStream} to print to; never {@code null}
	 * @return this {@code Executions} object for method chaining; never {@code null}
	 */
	@SuppressWarnings("DefaultCharset")
	public Executions debug(OutputStream out) {
		Preconditions.notNull(out, "OutputStream must not be null");
		debug(new PrintWriter(out, true));
		return this;
	}

	/**
	 * Print all executions to the supplied {@link Writer}.
	 *
	 * @param writer the {@code Writer} to print to; never {@code null}
	 * @return this {@code Executions} object for method chaining; never {@code null}
	 */
	@SuppressWarnings("DefaultCharset")
	public Executions debug(Writer writer) {
		Preconditions.notNull(writer, "Writer must not be null");
		debug(new PrintWriter(writer, true));
		return this;
	}

	private Executions debug(PrintWriter printWriter) {
		printWriter.println(this.category + " Executions:");
		this.executions.forEach(execution -> printWriter.printf("\t%s%n", execution));
		return this;
	}

	// --- Internals -----------------------------------------------------------

	private Stream<Execution> finishedExecutions() {
		return executionsByTerminationInfo(TerminationInfo::executed);
	}

	private Stream<Execution> finishedExecutionsByStatus(Status status) {
		Preconditions.notNull(status, "Status must not be null");
		return finishedExecutions()//
				.filter(execution -> execution.getTerminationInfo().getExecutionResult().getStatus() == status);
	}

	private Stream<Execution> executionsByTerminationInfo(Predicate<TerminationInfo> predicate) {
		return filter(execution -> predicate.test(execution.getTerminationInfo()));
	}

	/**
	 * Create executions from the supplied list of events.
	 */
	private static List<Execution> createExecutions(List<Event> events) {
		List<Execution> executions = new ArrayList<>();
		Map<TestDescriptor, Instant> executionStarts = new HashMap<>();

		for (Event event : events) {
			switch (event.getType()) {
				case STARTED -> executionStarts.put(event.getTestDescriptor(), event.getTimestamp());
				case SKIPPED -> {
					// Based on the Javadoc for EngineExecutionListener.executionSkipped(...),
					// a skipped descriptor must never be reported as started or finished,
					// but just in case a TestEngine does not adhere to that contract, we
					// make an attempt to remove any tracked "started" event.
					executionStarts.remove(event.getTestDescriptor());

					// Use the same timestamp for both the start and end Instant values.
					Instant timestamp = event.getTimestamp();

					Execution skippedEvent = Execution.skipped(event.getTestDescriptor(), timestamp, timestamp,
						event.getRequiredPayload(String.class));
					executions.add(skippedEvent);
				}
				case FINISHED -> {
					Instant startInstant = executionStarts.remove(event.getTestDescriptor());
					Instant endInstant = event.getTimestamp();

					// If "started" events have already been filtered out, it is no
					// longer possible to create a legitimate Execution for the current
					// descriptor. So we effectively ignore it in that case.
					if (startInstant != null) {
						Execution finishedEvent = Execution.finished(event.getTestDescriptor(), startInstant,
							endInstant, event.getRequiredPayload(TestExecutionResult.class));
						executions.add(finishedEvent);
					}
				}
				default -> {
					// Ignore other events
				}
			}
		}

		return executions;
	}

}
