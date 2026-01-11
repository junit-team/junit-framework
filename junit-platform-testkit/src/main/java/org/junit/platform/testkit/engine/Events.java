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

import static java.util.Collections.sort;
import static java.util.function.Predicate.isEqual;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.testkit.engine.Event.byPayload;
import static org.junit.platform.testkit.engine.Event.byType;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Index;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.opentest4j.AssertionFailedError;

/**
 * {@code Events} is a facade that provides a fluent API for working with
 * {@linkplain Event events}.
 *
 * @since 1.4
 */
@API(status = MAINTAINED, since = "1.7")
public final class Events {

	private final List<Event> events;
	private final String category;

	Events(Stream<Event> events, String category) {
		this(Preconditions.notNull(events, "Event stream must not be null").toList(), category);
	}

	Events(List<Event> events, String category) {
		Preconditions.notNull(events, "Event list must not be null");
		Preconditions.containsNoNullElements(events, "Event list must not contain null elements");

		this.events = List.copyOf(events);
		this.category = category;
	}

	String getCategory() {
		return this.category;
	}

	// --- Accessors -----------------------------------------------------------

	/**
	 * Get the {@linkplain Event events} as a {@link List}.
	 *
	 * @return the list of events; never {@code null}
	 * @see #stream()
	 */
	public List<Event> list() {
		return this.events;
	}

	/**
	 * Get the {@linkplain Event events} as a {@link Stream}.
	 *
	 * @return the stream of events; never {@code null}
	 * @see #list()
	 */
	public Stream<Event> stream() {
		return this.events.stream();
	}

	/**
	 * Shortcut for {@code events.stream().map(mapper)}.
	 *
	 * @param mapper a {@code Function} to apply to each event; never {@code null}
	 * @return the mapped stream of events; never {@code null}
	 * @see #stream()
	 * @see Stream#map(Function)
	 */
	public <R> Stream<R> map(Function<? super Event, ? extends R> mapper) {
		Preconditions.notNull(mapper, "Mapping function must not be null");
		return stream().map(mapper);
	}

	/**
	 * Shortcut for {@code events.stream().filter(predicate)}.
	 *
	 * @param predicate a {@code Predicate} to apply to each event to decide if
	 * it should be included in the filtered stream; never {@code null}
	 * @return the filtered stream of events; never {@code null}
	 * @see #stream()
	 * @see Stream#filter(Predicate)
	 */
	public Stream<Event> filter(Predicate<? super Event> predicate) {
		Preconditions.notNull(predicate, "Filter predicate must not be null");
		return stream().filter(predicate);
	}

	/**
	 * Get the {@link Executions} for the current set of {@linkplain Event events}.
	 *
	 * @return an instance of {@code Executions} for the current set of events;
	 * never {@code null}
	 */
	public Executions executions() {
		return new Executions(this.events, this.category);
	}

	// --- Statistics ----------------------------------------------------------

	/**
	 * Get the number of {@linkplain Event events} contained in this {@code Events}
	 * object.
	 */
	public long count() {
		return this.events.size();
	}

	// --- Built-in Filters ----------------------------------------------------

	/**
	 * Get the skipped {@link Events} contained in this {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 */
	public Events skipped() {
		return new Events(eventsByType(EventType.SKIPPED), this.category + " Skipped");
	}

	/**
	 * Get the started {@link Events} contained in this {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 */
	public Events started() {
		return new Events(eventsByType(EventType.STARTED), this.category + " Started");
	}

	/**
	 * Get the finished {@link Events} contained in this {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 */
	public Events finished() {
		return new Events(eventsByType(EventType.FINISHED), this.category + " Finished");
	}

	/**
	 * Get the aborted {@link Events} contained in this {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 */
	public Events aborted() {
		return new Events(finishedEventsByStatus(Status.ABORTED), this.category + " Aborted");
	}

	/**
	 * Get the succeeded {@link Events} contained in this {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 */
	public Events succeeded() {
		return new Events(finishedEventsByStatus(Status.SUCCESSFUL), this.category + " Successful");
	}

	/**
	 * Get the failed {@link Events} contained in this {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 */
	public Events failed() {
		return new Events(finishedEventsByStatus(Status.FAILED), this.category + " Failed");
	}

	/**
	 * Get the reporting entry publication {@link Events} contained in this
	 * {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 */
	public Events reportingEntryPublished() {
		return new Events(eventsByType(EventType.REPORTING_ENTRY_PUBLISHED),
			this.category + " Reporting Entry Published");
	}

	/**
	 * Get the file entry publication {@link Events} contained in this
	 * {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 * @since 1.12
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	public Events fileEntryPublished() {
		return new Events(eventsByType(EventType.FILE_ENTRY_PUBLISHED), this.category + " File Entry Published");
	}

	/**
	 * Get the dynamic registration {@link Events} contained in this
	 * {@code Events} object.
	 *
	 * @return the filtered {@code Events}; never {@code null}
	 */
	public Events dynamicallyRegistered() {
		return new Events(eventsByType(EventType.DYNAMIC_TEST_REGISTERED), this.category + " Dynamically Registered");
	}

	// --- Assertions ----------------------------------------------------------

	/**
	 * Assert statistics for the {@linkplain Event events} contained in this
	 * {@code Events} object.
	 *
	 * <h4>Example</h4>
	 *
	 * <p>{@code events.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));}
	 *
	 * @param statisticsConsumer a {@link Consumer} of {@link EventStatistics};
	 * never {@code null}
	 * @return this {@code Events} object for method chaining; never {@code null}
	 */
	public Events assertStatistics(Consumer<EventStatistics> statisticsConsumer) {
		Preconditions.notNull(statisticsConsumer, "Consumer must not be null");
		EventStatistics eventStatistics = new EventStatistics(this, this.category);
		statisticsConsumer.accept(eventStatistics);
		eventStatistics.assertAll();
		return this;
	}

	/**
	 * Assert that all {@linkplain Event events} contained in this {@code Events}
	 * object exactly match the provided conditions.
	 *
	 * <p>Conditions can be imported statically from {@link EventConditions}
	 * and {@link TestExecutionResultConditions}.
	 *
	 * <h4>Example</h4>
	 *
	 * <pre class="code">
	 * executionResults.testEvents().assertEventsMatchExactly(
	 *     event(test("exampleTestMethod"), started()),
	 *     event(test("exampleTestMethod"), finishedSuccessfully())
	 * );
	 * </pre>
	 *
	 * @param conditions the conditions to match against; never {@code null}
	 * @see #assertEventsMatchLoosely(Condition...)
	 * @see #assertEventsMatchLooselyInOrder(Condition...)
	 * @see EventConditions
	 * @see TestExecutionResultConditions
	 */
	@SafeVarargs
	public final void assertEventsMatchExactly(Condition<? super Event>... conditions) {
		Preconditions.notNull(conditions, "conditions must not be null");
		assertEventsMatchExactly(this.events, conditions);
	}

	/**
	 * Assert that all provided conditions are matched by an {@linkplain Event event}
	 * contained in this {@code Events} object, regardless of order.
	 *
	 * <p>Note that this method performs a partial match. Thus, some events may
	 * not match any of the provided conditions.
	 *
	 * <p>Conditions can be imported statically from {@link EventConditions}
	 * and {@link TestExecutionResultConditions}.
	 *
	 * <h4>Example</h4>
	 *
	 * <pre class="code">
	 * executionResults.testEvents().assertEventsMatchLoosely(
	 *     event(test("exampleTestMethod"), started()),
	 *     event(test("exampleTestMethod"), finishedSuccessfully())
	 * );
	 * </pre>
	 *
	 * @param conditions the conditions to match against; never {@code null}
	 * @since 1.7
	 * @see #assertEventsMatchExactly(Condition...)
	 * @see #assertEventsMatchLooselyInOrder(Condition...)
	 * @see EventConditions
	 * @see TestExecutionResultConditions
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final void assertEventsMatchLoosely(Condition<? super Event>... conditions) {
		Preconditions.notNull(conditions, "conditions must not be null");
		Preconditions.containsNoNullElements(conditions, "conditions must not contain null elements");
		assertEventsMatchLoosely(this.events, conditions);
	}

	/**
	 * Assert that all provided conditions are matched by an {@linkplain Event event}
	 * contained in this {@code Events} object.
	 *
	 * <p>Note that this method performs a partial match. Thus, some events may
	 * not match any of the provided conditions; however, the conditions provided
	 * must be in the correct order.
	 *
	 * <p>Conditions can be imported statically from {@link EventConditions}
	 * and {@link TestExecutionResultConditions}.
	 *
	 * <h4>Example</h4>
	 *
	 * <pre class="code">
	 * executionResults.testEvents().assertEventsMatchLooselyInOrder(
	 *     event(test("exampleTestMethod"), started()),
	 *     event(test("exampleTestMethod"), finishedSuccessfully())
	 * );
	 * </pre>
	 *
	 * @param conditions the conditions to match against; never {@code null}
	 * @since 1.7
	 * @see #assertEventsMatchExactly(Condition...)
	 * @see #assertEventsMatchLoosely(Condition...)
	 * @see EventConditions
	 * @see TestExecutionResultConditions
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final void assertEventsMatchLooselyInOrder(Condition<? super Event>... conditions) {
		Preconditions.notNull(conditions, "conditions must not be null");
		Preconditions.containsNoNullElements(conditions, "conditions must not contain null elements");
		assertEventsMatchLooselyInOrder(this.events, conditions);
	}

	/**
	 * Shortcut for {@code org.assertj.core.api.Assertions.assertThat(events.list())}.
	 *
	 * @return an instance of {@link ListAssert} for events; never {@code null}
	 * @see org.assertj.core.api.Assertions#assertThat(List)
	 * @see org.assertj.core.api.ListAssert
	 */
	public ListAssert<Event> assertThatEvents() {
		return org.assertj.core.api.Assertions.assertThat(list());
	}

	// --- Diagnostics ---------------------------------------------------------

	/**
	 * Print all events to {@link System#out}.
	 *
	 * @return this {@code Events} object for method chaining; never {@code null}
	 */
	public Events debug() {
		debug(System.out);
		return this;
	}

	/**
	 * Print all events to the supplied {@link OutputStream}.
	 *
	 * @param out the {@code OutputStream} to print to; never {@code null}
	 * @return this {@code Events} object for method chaining; never {@code null}
	 */
	@SuppressWarnings("DefaultCharset")
	public Events debug(OutputStream out) {
		Preconditions.notNull(out, "OutputStream must not be null");
		debug(new PrintWriter(out, true));
		return this;
	}

	/**
	 * Print all events to the supplied {@link Writer}.
	 *
	 * @param writer the {@code Writer} to print to; never {@code null}
	 * @return this {@code Events} object for method chaining; never {@code null}
	 */
	public Events debug(Writer writer) {
		Preconditions.notNull(writer, "Writer must not be null");
		debug(new PrintWriter(writer, true));
		return this;
	}

	private Events debug(PrintWriter printWriter) {
		printWriter.println(this.category + " Events:");
		this.events.forEach(event -> printWriter.printf("\t%s%n", event));
		return this;
	}

	// --- Internals -----------------------------------------------------------

	private Stream<Event> eventsByType(EventType type) {
		Preconditions.notNull(type, "EventType must not be null");
		return stream().filter(byType(type));
	}

	private Stream<Event> finishedEventsByStatus(Status status) {
		Preconditions.notNull(status, "Status must not be null");
		return eventsByType(EventType.FINISHED)//
				.filter(byPayload(TestExecutionResult.class, where(TestExecutionResult::getStatus, isEqual(status))));
	}

	@SafeVarargs
	private static void assertEventsMatchExactly(List<Event> events, Condition<? super Event>... conditions) {
		Assertions.assertThat(events).hasSize(conditions.length);

		SoftAssertions softly = new SoftAssertions();
		for (int i = 0; i < conditions.length; i++) {
			softly.assertThat(events).has(conditions[i], Index.atIndex(i));
		}
		softly.assertAll();
	}

	@SafeVarargs
	private static void assertEventsMatchLoosely(List<Event> events, Condition<? super Event>... conditions) {
		SoftAssertions softly = new SoftAssertions();
		for (Condition<? super Event> condition : conditions) {
			checkCondition(events, softly, condition);
		}
		softly.assertAll();
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private static void assertEventsMatchLooselyInOrder(List<Event> events, Condition<? super Event>... conditions) {
		Assertions.assertThat(conditions).hasSizeLessThanOrEqualTo(events.size());
		SoftAssertions softly = new SoftAssertions();

		// @formatter:off
		List<Integer> indices = Arrays.stream(conditions)
				.map(condition -> findEvent(events, softly, condition))
				.filter(Objects::nonNull)
				.map(events::indexOf)
				.toList();
		// @formatter:on

		if (isNotInIncreasingOrder(indices)) {
			throw new AssertionFailedError("Conditions are not in the correct order.");
		}

		softly.assertAll();
	}

	private static boolean isNotInIncreasingOrder(List<Integer> indices) {
		List<Integer> copy = new ArrayList<>(indices);
		sort(copy);

		return !indices.equals(copy);
	}

	private static void checkCondition(List<Event> events, SoftAssertions softly, Condition<? super Event> condition) {
		if (events.stream().noneMatch(condition::matches)) {
			softly.fail("Condition did not match any event: " + condition);
		}
	}

	private static @Nullable Event findEvent(List<Event> events, SoftAssertions softly,
			Condition<? super Event> condition) {
		// @formatter:off
		Optional<Event> matchedEvent = events.stream()
				.filter(condition::matches)
				.findFirst();
		// @formatter:on

		if (matchedEvent.isEmpty()) {
			softly.fail("Condition did not match any event: " + condition);
		}

		return matchedEvent.orElse(null);
	}

}
