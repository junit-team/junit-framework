/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Timeout.TIMEOUT_MODE_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;
import static org.junit.jupiter.engine.Constants.DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.util.RuntimeUtils;
import org.mockito.stubbing.Answer;

/**
 * @since 5.5
 */
class TimeoutConfigurationTests {

	ExtensionContext extensionContext = mock();
	TimeoutConfiguration config = new TimeoutConfiguration(extensionContext);

	@Test
	void notDisabledByDefault() {
		assertThat(config.isTimeoutDisabled()).isFalse();
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			Enabled,           false
			Disabled,          true
			Disabled_ON_debug, false
			""")
	void disabledBasedOnTimeoutMode(String timeoutMode, boolean disabled) {
		when(extensionContext.getConfigurationParameter(eq(TIMEOUT_MODE_PROPERTY_NAME), any())) //
				.thenAnswer(callConverter(timeoutMode));

		config = new TimeoutConfiguration(extensionContext);

		assertThat(config.isTimeoutDisabled()) //
				.isEqualTo("disabled_on_debug".equalsIgnoreCase(timeoutMode) ? RuntimeUtils.isDebugMode() : disabled);
	}

	@Test
	void noTimeoutIfNoPropertiesAreSet() {
		assertThat(config.getDefaultBeforeAllMethodTimeout()).isEmpty();
		assertThat(config.getDefaultBeforeEachMethodTimeout()).isEmpty();
		assertThat(config.getDefaultTestMethodTimeout()).isEmpty();
		assertThat(config.getDefaultTestTemplateMethodTimeout()).isEmpty();
		assertThat(config.getDefaultTestFactoryMethodTimeout()).isEmpty();
		assertThat(config.getDefaultAfterEachMethodTimeout()).isEmpty();
		assertThat(config.getDefaultAfterAllMethodTimeout()).isEmpty();
		assertThat(config.getDefaultTimeoutThreadMode()).isEmpty();
	}

	@Test
	void defaultTimeoutIsUsedUnlessAMoreSpecificOneIsSet() {
		when(extensionContext.getConfigurationParameter(DEFAULT_TIMEOUT_PROPERTY_NAME)).thenReturn(Optional.of("42"));

		assertThat(config.getDefaultBeforeAllMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultBeforeEachMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultTestMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultTestTemplateMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultTestFactoryMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultAfterEachMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultAfterAllMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
	}

	@Test
	void defaultCategoryTimeoutIsUsedUnlessAMoreSpecificOneIsSet() {
		when(extensionContext.getConfigurationParameter(DEFAULT_TIMEOUT_PROPERTY_NAME)).thenReturn(Optional.of("2"));
		when(extensionContext.getConfigurationParameter(DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("3"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("5"));

		assertThat(config.getDefaultBeforeAllMethodTimeout()).contains(new TimeoutDuration(3, SECONDS));
		assertThat(config.getDefaultBeforeEachMethodTimeout()).contains(new TimeoutDuration(3, SECONDS));
		assertThat(config.getDefaultTestMethodTimeout()).contains(new TimeoutDuration(5, SECONDS));
		assertThat(config.getDefaultTestTemplateMethodTimeout()).contains(new TimeoutDuration(5, SECONDS));
		assertThat(config.getDefaultTestFactoryMethodTimeout()).contains(new TimeoutDuration(5, SECONDS));
		assertThat(config.getDefaultAfterEachMethodTimeout()).contains(new TimeoutDuration(3, SECONDS));
		assertThat(config.getDefaultAfterAllMethodTimeout()).contains(new TimeoutDuration(3, SECONDS));
	}

	@Test
	void specificTimeoutsAreUsedIfSet() {
		when(extensionContext.getConfigurationParameter(DEFAULT_TIMEOUT_PROPERTY_NAME)).thenReturn(Optional.of("2"));
		when(extensionContext.getConfigurationParameter(DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("3"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("5"));
		when(extensionContext.getConfigurationParameter(DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("7ns"));
		when(extensionContext.getConfigurationParameter(DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("11μs"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("13ms"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("17s"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("19m"));
		when(extensionContext.getConfigurationParameter(DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("23h"));
		when(extensionContext.getConfigurationParameter(DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("29d"));

		assertThat(config.getDefaultBeforeAllMethodTimeout()).contains(new TimeoutDuration(7, NANOSECONDS));
		assertThat(config.getDefaultBeforeEachMethodTimeout()).contains(new TimeoutDuration(11, MICROSECONDS));
		assertThat(config.getDefaultTestMethodTimeout()).contains(new TimeoutDuration(13, MILLISECONDS));
		assertThat(config.getDefaultTestTemplateMethodTimeout()).contains(new TimeoutDuration(17, SECONDS));
		assertThat(config.getDefaultTestFactoryMethodTimeout()).contains(new TimeoutDuration(19, MINUTES));
		assertThat(config.getDefaultAfterEachMethodTimeout()).contains(new TimeoutDuration(23, HOURS));
		assertThat(config.getDefaultAfterAllMethodTimeout()).contains(new TimeoutDuration(29, DAYS));
	}

	@Test
	void logsInvalidValues(@TrackLogRecords LogRecordListener logRecordListener) {
		when(extensionContext.getConfigurationParameter(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("invalid"));

		assertThat(config.getDefaultTestMethodTimeout()).isEmpty();
		assertThat(logRecordListener.stream(Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly(
					"Ignored invalid timeout 'invalid' set via the 'junit.jupiter.execution.timeout.test.method.default' configuration parameter.");
	}

	@Test
	void specificThreadModeIsUsed() {
		when(extensionContext.getConfigurationParameter(eq(DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME), any())) //
				.thenAnswer(callConverter("SEPARATE_THREAD"));

		assertThat(config.getDefaultTimeoutThreadMode()).contains(SEPARATE_THREAD);
	}

	@Test
	void logsInvalidThreadModeValueAndReturnEmpty() {
		when(extensionContext.getConfigurationParameter(eq(DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME), any())) //
				.thenAnswer(callConverter("invalid"));

		assertThatThrownBy(() -> config.getDefaultTimeoutThreadMode()) //
				.hasMessage(
					"Invalid timeout thread mode 'INVALID' set via the 'junit.jupiter.execution.timeout.thread.mode.default' configuration parameter.");
	}

	@SuppressWarnings("unchecked")
	private static Answer<?> callConverter(String value) {
		return invocation -> Optional.ofNullable(invocation.getArgument(1, Function.class).apply(value));
	}
}
