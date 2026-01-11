/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code DiscoveryIssue} represents an issue that was encountered during test
 * discovery by a {@link TestEngine}.
 *
 * @since 1.13
 */
@API(status = EXPERIMENTAL, since = "6.0")
public interface DiscoveryIssue {

	/**
	 * Create a new {@code DiscoveryIssue} with the supplied {@link Severity} and
	 * message.
	 *
	 * @param severity the severity of the issue; never {@code null}
	 * @param message the message of the issue; never blank
	 * @see #builder(Severity, String)
	 */
	static DiscoveryIssue create(Severity severity, String message) {
		return builder(severity, message).build();
	}

	/**
	 * Create a new {@link Builder} for creating a {@code DiscoveryIssue} with
	 * the supplied {@link Severity} and message.
	 *
	 * @param severity the severity of the issue; never {@code null}
	 * @param message the message of the issue; never blank
	 * @see Builder
	 * @see #create(Severity, String)
	 */
	static Builder builder(Severity severity, String message) {
		Preconditions.notNull(severity, "severity must not be null");
		Preconditions.notBlank(message, "message must not be blank");
		return new DefaultDiscoveryIssue.Builder(severity, message);
	}

	/**
	 * {@return the severity of this issue}
	 */
	Severity severity();

	/**
	 * {@return the message of this issue}
	 */
	String message();

	/**
	 * {@return the source of this issue}
	 */
	Optional<TestSource> source();

	/**
	 * {@return the cause of this issue}
	 */
	Optional<Throwable> cause();

	/**
	 * Create a copy of this issue with the modified message produced by the
	 * supplied operator.
	 */
	default DiscoveryIssue withMessage(UnaryOperator<String> messageModifier) {
		String oldMessage = message();
		String newMessage = messageModifier.apply(oldMessage);
		if (oldMessage.equals(newMessage)) {
			return this;
		}
		return DiscoveryIssue.builder(severity(), newMessage) //
				.source(source()) //
				.cause(cause()) //
				.build();
	}

	/**
	 * The severity of a {@code DiscoveryIssue}.
	 */
	enum Severity {

		/**
		 * Indicates that the engine encountered something that could be
		 * potentially problematic, but could also happen due to a valid setup
		 * or configuration.
		 */
		INFO,

		/**
		 * Indicates that the engine encountered something that is problematic
		 * and might lead to unexpected behavior or will be removed or changed
		 * in a future release.
		 */
		WARNING,

		/**
		 * Indicates that the engine encountered something that is definitely
		 * problematic and will lead to unexpected behavior.
		 */
		ERROR
	}

	/**
	 * Builder for creating a {@code DiscoveryIssue}.
	 */
	interface Builder {

		/**
		 * Set the {@link TestSource} for the {@code DiscoveryIssue}.
		 *
		 * @param source the {@link TestSource} for the {@code DiscoveryIssue};
		 * never {@code null} but potentially empty
		 */
		default Builder source(Optional<TestSource> source) {
			source.ifPresent(this::source);
			return this;
		}

		/**
		 * Set the {@link TestSource} for the {@code DiscoveryIssue}.
		 *
		 * @param source the {@link TestSource} for the {@code DiscoveryIssue};
		 * may be {@code null}
		 */
		Builder source(@Nullable TestSource source);

		/**
		 * Set the {@link Throwable} that caused the {@code DiscoveryIssue}.
		 *
		 * @param cause the {@link Throwable} that caused the
		 * {@code DiscoveryIssue}; never {@code null} but potentially empty
		 */
		default Builder cause(Optional<Throwable> cause) {
			cause.ifPresent(this::cause);
			return this;
		}

		/**
		 * Set the {@link Throwable} that caused the {@code DiscoveryIssue}.
		 *
		 * @param cause the {@link Throwable} that caused the
		 * {@code DiscoveryIssue}; may be {@code null}
		 */
		Builder cause(@Nullable Throwable cause);

		/**
		 * Build the {@code DiscoveryIssue}.
		 */
		DiscoveryIssue build();

	}
}
