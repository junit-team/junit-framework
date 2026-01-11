/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link ConditionEvaluationResult}.
 *
 * @since 5.13.3
 */
class ConditionEvaluationResultTests {

	@Test
	void enabledWithReason() {
		var result = ConditionEvaluationResult.enabled("reason");

		assertThat(result.isDisabled()).isFalse();
		assertThat(result.getReason()).contains("reason");
		assertThat(result).asString()//
				.isEqualTo("ConditionEvaluationResult [enabled = true, reason = 'reason']");
	}

	@BlankReasonsTest
	void enabledWithBlankReason(@Nullable String reason) {
		var result = ConditionEvaluationResult.enabled(reason);

		assertThat(result.isDisabled()).isFalse();
		assertThat(result.getReason()).isEmpty();
		assertThat(result).asString()//
				.isEqualTo("ConditionEvaluationResult [enabled = true, reason = '<unknown>']");
	}

	@Test
	void disabledWithDefaultReason() {
		var result = ConditionEvaluationResult.disabled("default");

		assertThat(result.isDisabled()).isTrue();
		assertThat(result.getReason()).contains("default");
		assertThat(result).asString()//
				.isEqualTo("ConditionEvaluationResult [enabled = false, reason = 'default']");
	}

	@BlankReasonsTest
	void disabledWithBlankDefaultReason(@Nullable String reason) {
		var result = ConditionEvaluationResult.disabled(reason);

		assertThat(result.isDisabled()).isTrue();
		assertThat(result.getReason()).isEmpty();
		assertThat(result).asString()//
				.isEqualTo("ConditionEvaluationResult [enabled = false, reason = '<unknown>']");
	}

	@BlankReasonsTest
	void disabledWithDefaultReasonAndBlankCustomReason(@Nullable String customReason) {
		var result = ConditionEvaluationResult.disabled("default", customReason);

		assertThat(result.isDisabled()).isTrue();
		assertThat(result.getReason()).contains("default");
		assertThat(result).asString()//
				.isEqualTo("ConditionEvaluationResult [enabled = false, reason = 'default']");
	}

	@BlankReasonsTest
	void disabledWithBlankDefaultReasonAndCustomReason(@Nullable String reason) {
		var result = ConditionEvaluationResult.disabled(reason, "custom");

		assertThat(result.isDisabled()).isTrue();
		assertThat(result.getReason()).contains("custom");
		assertThat(result).asString().isEqualTo("ConditionEvaluationResult [enabled = false, reason = 'custom']");
	}

	@BlankReasonsTest
	void disabledWithBlankDefaultReasonAndBlankCustomReason(@Nullable String reason) {
		// We intentionally use the reason as both the default and custom reason.
		var result = ConditionEvaluationResult.disabled(reason, reason);

		assertThat(result.isDisabled()).isTrue();
		assertThat(result.getReason()).isEmpty();
		assertThat(result).asString()//
				.isEqualTo("ConditionEvaluationResult [enabled = false, reason = '<unknown>']");
	}

	@Test
	void disabledWithDefaultReasonAndCustomReason() {
		disabledWithDefaultReasonAndCustomReason("default", "custom");
	}

	@Test
	void disabledWithDefaultReasonAndCustomReasonWithLeadingAndTrailingWhitespace() {
		disabledWithDefaultReasonAndCustomReason("   default   ", "   custom   ");
	}

	private static void disabledWithDefaultReasonAndCustomReason(String defaultReason, String customReason) {
		var result = ConditionEvaluationResult.disabled(defaultReason, customReason);

		assertThat(result.isDisabled()).isTrue();
		assertThat(result.getReason()).contains("default ==> custom");
		assertThat(result).asString()//
				.isEqualTo("ConditionEvaluationResult [enabled = false, reason = 'default ==> custom']");
	}

	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "   ", "\t", "\f", "\r", "\n", "\r\n" })
	@interface BlankReasonsTest {
	}

}
