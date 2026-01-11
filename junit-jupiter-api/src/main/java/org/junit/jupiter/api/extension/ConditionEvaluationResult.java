/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.STABLE;

import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.commons.util.ToStringBuilder;

/**
 * The result of evaluating an {@link ExecutionCondition}.
 *
 * @since 5.0
 */
@API(status = STABLE, since = "5.0")
public class ConditionEvaluationResult {

	/**
	 * Factory for creating <em>enabled</em> results.
	 *
	 * @param reason the reason why the container or test should be enabled; may
	 * be {@code null} or <em>blank</em> if the reason is unknown
	 * @return an enabled {@code ConditionEvaluationResult} with the given reason
	 * or an <em>empty</em> reason if the reason is unknown
	 * @see StringUtils#isBlank(String)
	 */
	public static ConditionEvaluationResult enabled(@Nullable String reason) {
		return new ConditionEvaluationResult(true, reason);
	}

	/**
	 * Factory for creating <em>disabled</em> results.
	 *
	 * @param reason the reason why the container or test should be disabled; may
	 * be {@code null} or <em>blank</em> if the reason is unknown
	 * @return a disabled {@code ConditionEvaluationResult} with the given reason
	 * or an <em>empty</em> reason if the reason is unknown
	 * @see StringUtils#isBlank(String)
	 */
	public static ConditionEvaluationResult disabled(@Nullable String reason) {
		return new ConditionEvaluationResult(false, reason);
	}

	/**
	 * Factory for creating <em>disabled</em> results with custom reasons
	 * added by the user.
	 *
	 * <p>If non-blank default and custom reasons are provided, they will be
	 * concatenated using the format: <code>"reason&nbsp;==&gt;&nbsp;customReason"</code>.
	 *
	 * @param reason the default reason why the container or test should be disabled;
	 * may be {@code null} or <em>blank</em> if the default reason is unknown
	 * @param customReason the custom reason why the container or test should be
	 * disabled; may be {@code null} or <em>blank</em> if the custom reason is unknown
	 * @return a disabled {@code ConditionEvaluationResult} with the given reason(s)
	 * or an <em>empty</em> reason if the reasons are unknown
	 * @since 5.7
	 * @see StringUtils#isBlank(String)
	 */
	@API(status = STABLE, since = "5.7")
	public static ConditionEvaluationResult disabled(@Nullable String reason, @Nullable String customReason) {
		if (StringUtils.isBlank(reason)) {
			return disabled(customReason);
		}
		if (StringUtils.isBlank(customReason)) {
			return disabled(reason);
		}
		return disabled("%s ==> %s".formatted(reason.strip(), customReason.strip()));
	}

	private final boolean enabled;

	private final Optional<String> reason;

	private ConditionEvaluationResult(boolean enabled, @Nullable String reason) {
		this.enabled = enabled;
		this.reason = StringUtils.isNotBlank(reason) ? Optional.of(reason.strip()) : Optional.empty();
	}

	/**
	 * Whether the container or test should be disabled.
	 *
	 * @return {@code true} if the container or test should be disabled
	 */
	public boolean isDisabled() {
		return !this.enabled;
	}

	/**
	 * Get the reason why the container or test should be enabled or disabled,
	 * if available.
	 */
	public Optional<String> getReason() {
		return this.reason;
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("enabled", this.enabled)
				.append("reason", this.reason.orElse("<unknown>"))
				.toString();
		// @formatter:on
	}

}
