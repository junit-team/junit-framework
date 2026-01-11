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

import static org.apiguardian.api.API.Status.STABLE;

import java.util.Optional;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * The result of applying a {@link Filter}.
 *
 * @since 1.0
 */
@API(status = STABLE, since = "1.0")
public class FilterResult {

	/**
	 * Factory for creating <em>included</em> results.
	 *
	 * @param reason the reason why the filtered object was included
	 * @return an included {@code FilterResult} with the given reason
	 */
	public static FilterResult included(@Nullable String reason) {
		return new FilterResult(true, reason);
	}

	/**
	 * Factory for creating <em>excluded</em> results.
	 *
	 * @param reason the reason why the filtered object was excluded
	 * @return an excluded {@code FilterResult} with the given reason
	 */
	public static FilterResult excluded(@Nullable String reason) {
		return new FilterResult(false, reason);
	}

	/**
	 * Factory for creating filter results based on the condition given.
	 *
	 * @param included whether the filtered object should be included
	 * @return a valid {@code FilterResult} for the given condition
	 */
	public static FilterResult includedIf(boolean included) {
		return includedIf(included, () -> null, () -> null);
	}

	/**
	 * Factory for creating filter results based on the condition given.
	 *
	 * @param included whether the filtered object should be included
	 * @param inclusionReasonSupplier supplier for the reason in case of inclusion
	 * @param exclusionReasonSupplier supplier for the reason in case of exclusion
	 * @return a valid {@code FilterResult} for the given condition
	 */
	public static FilterResult includedIf(boolean included, Supplier<@Nullable String> inclusionReasonSupplier,
			Supplier<@Nullable String> exclusionReasonSupplier) {
		return included ? included(inclusionReasonSupplier.get()) : excluded(exclusionReasonSupplier.get());
	}

	private final boolean included;

	private final Optional<String> reason;

	private FilterResult(boolean included, @Nullable String reason) {
		this.included = included;
		this.reason = Optional.ofNullable(reason);
	}

	/**
	 * {@return {@code true} if the filtered object should be included}
	 */
	public boolean included() {
		return this.included;
	}

	/**
	 * {@return {@code true} if the filtered object should be excluded}
	 */
	public boolean excluded() {
		return !included();
	}

	/**
	 * Get the reason why the filtered object should be included or excluded,
	 * if available.
	 */
	public Optional<String> getReason() {
		return this.reason;
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("included", this.included)
				.append("reason", this.reason.orElse("<unknown>"))
				.toString();
		// @formatter:on
	}

}
