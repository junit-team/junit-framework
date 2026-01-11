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

import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * @since 1.13
 */
final class DefaultDiscoveryIssue implements DiscoveryIssue {

	private final Severity severity;
	private final String message;

	private final @Nullable TestSource source;

	private final @Nullable Throwable cause;

	DefaultDiscoveryIssue(Builder builder) {
		this.severity = builder.severity;
		this.message = builder.message;
		this.source = builder.source;
		this.cause = builder.cause;
	}

	@Override
	public Severity severity() {
		return this.severity;
	}

	@Override
	public String message() {
		return this.message;
	}

	@Override
	public Optional<TestSource> source() {
		return Optional.ofNullable(this.source);
	}

	@Override
	public Optional<Throwable> cause() {
		return Optional.ofNullable(this.cause);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultDiscoveryIssue that = (DefaultDiscoveryIssue) o;
		return this.severity == that.severity //
				&& Objects.equals(this.message, that.message) //
				&& Objects.equals(this.source, that.source) //
				&& Objects.equals(this.cause, that.cause);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.severity, this.message, this.source, this.cause);
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(DiscoveryIssue.class.getSimpleName()) //
				.append("severity", this.severity) //
				.append("message", this.message);
		if (this.source != null) {
			builder.append("source", this.source);
		}
		if (this.cause != null) {
			builder.append("cause", this.cause);
		}
		return builder.toString();
	}

	static class Builder implements DiscoveryIssue.Builder {

		private final Severity severity;
		private final String message;

		@Nullable
		private TestSource source;

		@Nullable
		public Throwable cause;

		Builder(Severity severity, String message) {
			this.severity = severity;
			this.message = message;
		}

		@Override
		public Builder source(@Nullable TestSource source) {
			this.source = source;
			return this;
		}

		@Override
		public Builder cause(@Nullable Throwable cause) {
			this.cause = cause;
			return this;
		}

		@Override
		public DiscoveryIssue build() {
			return new DefaultDiscoveryIssue(this);
		}
	}
}
