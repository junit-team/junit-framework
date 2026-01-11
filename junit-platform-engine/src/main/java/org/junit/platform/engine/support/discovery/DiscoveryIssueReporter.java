/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.EngineDiscoveryListener;
import org.junit.platform.engine.UniqueId;

/**
 * {@code DiscoveryIssueReporter} defines the API for reporting
 * {@link DiscoveryIssue DiscoveryIssues}.
 *
 * <p>This interface is not intended to be implemented by clients.
 *
 * @since 1.13
 * @see SelectorResolver.Context
 */
@API(status = EXPERIMENTAL, since = "6.0")
public interface DiscoveryIssueReporter {

	/**
	 * Create a new {@code DiscoveryIssueReporter} that reports issues to the
	 * supplied {@link EngineDiscoveryListener} for the specified engine.
	 *
	 * @param engineDiscoveryListener the listener to report issues to; never
	 * {@code null}
	 * @param engineId the unique identifier of the engine; never {@code null}
	 */
	static DiscoveryIssueReporter forwarding(EngineDiscoveryListener engineDiscoveryListener, UniqueId engineId) {
		Preconditions.notNull(engineDiscoveryListener, "engineDiscoveryListener must not be null");
		Preconditions.notNull(engineId, "engineId must not be null");
		return issue -> engineDiscoveryListener.issueEncountered(engineId, issue);
	}

	/**
	 * Create a new {@code DiscoveryIssueReporter} that adds reported issues to
	 * the supplied collection.
	 *
	 * @param collection the collection to add issues to; never {@code null}
	 */
	static DiscoveryIssueReporter collecting(Collection<? super DiscoveryIssue> collection) {
		Preconditions.notNull(collection, "collection must not be null");
		return consuming(collection::add);
	}

	/**
	 * Create a new {@code DiscoveryIssueReporter} that adds reported issues to
	 * the supplied consumer.
	 *
	 * @param consumer the consumer to report issues to; never {@code null}
	 */
	static DiscoveryIssueReporter consuming(Consumer<? super DiscoveryIssue> consumer) {
		Preconditions.notNull(consumer, "consumer must not be null");
		return consumer::accept;
	}

	/**
	 * Create a new {@code DiscoveryIssueReporter} that avoids reporting
	 * duplicate issues.
	 *
	 * <p>The implementation returned by this method is not thread-safe.
	 *
	 * @param delegate the delegate to forward issues to; never {@code null}
	 */
	static DiscoveryIssueReporter deduplicating(DiscoveryIssueReporter delegate) {
		Preconditions.notNull(delegate, "delegate must not be null");
		Set<DiscoveryIssue> seen = new HashSet<>();
		return issue -> {
			boolean notSeen = seen.add(issue);
			if (notSeen) {
				delegate.reportIssue(issue);
			}
		};
	}

	/**
	 * Build the supplied {@link DiscoveryIssue.Builder Builder} and report the
	 * resulting {@link DiscoveryIssue}.
	 */
	default void reportIssue(DiscoveryIssue.Builder builder) {
		reportIssue(builder.build());
	}

	/**
	 * Report the supplied {@link DiscoveryIssue}.
	 */
	void reportIssue(DiscoveryIssue issue);

	/**
	 * Create a {@link Condition} that reports a {@link DiscoveryIssue} when the
	 * supplied {@link Predicate} is not met.
	 *
	 * @param predicate the predicate to test; never {@code null}
	 * @param issueCreator the function to create the issue with; never {@code null}
	 * @return a new {@code Condition}; never {@code null}
	 */
	default <T> Condition<T> createReportingCondition(Predicate<T> predicate,
			Function<T, DiscoveryIssue> issueCreator) {
		Preconditions.notNull(predicate, "predicate must not be null");
		Preconditions.notNull(issueCreator, "issueCreator must not be null");
		return value -> {
			if (predicate.test(value)) {
				return true;
			}
			else {
				reportIssue(issueCreator.apply(value));
				return false;
			}
		};
	}

	/**
	 * A {@code Condition} is a union of {@link Predicate} and {@link Consumer}.
	 *
	 * <p>Instances of this type may be used as {@link Predicate Predicates} or
	 * {@link Consumer Consumers}. For example, a {@code Condition} may be
	 * passed to {@link java.util.stream.Stream#filter(Predicate)} if it is used
	 * for filtering, or to {@link java.util.stream.Stream#peek(Consumer)} if it
	 * is only used for reporting or other side effects.
	 *
	 * <p>This interface is not intended to be implemented by clients.
	 *
	 * @see #createReportingCondition(Predicate, Function)
	 */
	interface Condition<T> {

		/**
		 * Create a {@link Condition} that is always satisfied.
		 */
		static <T> Condition<T> alwaysSatisfied() {
			return __ -> true;
		}

		/**
		 * Evaluate this condition to potentially report an issue.
		 */
		boolean check(T value);

		/**
		 * Return a composed condition that represents a logical AND of this
		 * and the supplied condition.
		 *
		 * <p>The default implementation avoids short-circuiting so
		 * <em>both</em> conditions will be evaluated even if this condition
		 * returns {@code false} to ensure that all issues are reported.
		 *
		 * @return the composed condition; never {@code null}
		 */
		@SuppressWarnings("ShortCircuitBoolean")
		default Condition<T> and(Condition<? super T> that) {
			Preconditions.notNull(that, "condition must not be null");
			return value -> this.check(value) & that.check(value);
		}

		/**
		 * {@return this condition as a {@link Predicate}}
		 */
		default Predicate<T> toPredicate() {
			return this::check;
		}

		/**
		 * {@return this condition as a {@link Consumer}}
		 */
		default Consumer<T> toConsumer() {
			return this::check;
		}

	}
}
