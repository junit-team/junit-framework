/*
 * Copyright 2015-2025 the original author or authors.
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
	 * @param reason the reason why the container or test should be enabled
	 * @return an enabled {@code ConditionEvaluationResult} with the given reason
	 */
	public static ConditionEvaluationResult enabled(@Nullable String reason) {
		return new ConditionEvaluationResult(true, reason);
	}

	/**
	 * Factory for creating <em>disabled</em> results.
	 *
	 * @param reason the reason why the container or test should be disabled
	 * @return a disabled {@code ConditionEvaluationResult} with the given reason
	 */
	public static ConditionEvaluationResult disabled(@Nullable String reason) {
		return new ConditionEvaluationResult(false, reason);
	}

	/**
	 * Factory for creating <em>disabled</em> results with custom reasons
	 * added by the user.
	 *
	 * @param reason the default reason why the container or test should be disabled
	 * @param customReason the custom reason why the container or test should be disabled
	 * @return a disabled {@code ConditionEvaluationResult} with the given reasons
	 * @since 5.7
	 */
	@API(status = STABLE, since = "5.7")
	public static ConditionEvaluationResult disabled(String reason, String customReason) {
		if (StringUtils.isBlank(customReason)) {
			return disabled(reason);
		}
		return disabled("%s ==> %s".formatted(reason, customReason));
	}

	private final boolean enabled;

	private final Optional<String> reason;

	private ConditionEvaluationResult(boolean enabled, @Nullable String reason) {
		this.enabled = enabled;
		this.reason = Optional.ofNullable(reason);
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
