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

import static org.junit.jupiter.api.Timeout.DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME;
import static org.junit.jupiter.api.Timeout.TIMEOUT_MODE_PROPERTY_NAME;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.config.EnumConfigurationParameterConverter;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.RuntimeUtils;

/**
 * @since 5.5
 */
class TimeoutConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(TimeoutConfiguration.class);

	private final TimeoutDurationParser parser = new TimeoutDurationParser();
	private final Map<String, Optional<TimeoutDuration>> cache = new ConcurrentHashMap<>();
	private final AtomicReference<Optional<ThreadMode>> threadMode = new AtomicReference<>();
	private final ExtensionContext extensionContext;
	private final boolean timeoutDisabled;

	TimeoutConfiguration(ExtensionContext extensionContext) {
		this.extensionContext = extensionContext;
		this.timeoutDisabled = new EnumConfigurationParameterConverter<>(TimeoutMode.class, "timeout mode") //
				.get(extensionContext, TIMEOUT_MODE_PROPERTY_NAME) //
				.map(TimeoutMode::isTimeoutDisabled) //
				.orElse(false);
	}

	boolean isTimeoutDisabled() {
		return timeoutDisabled;
	}

	Optional<TimeoutDuration> getDefaultTestMethodTimeout() {
		return parseOrDefault(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultTestableMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultTestTemplateMethodTimeout() {
		return parseOrDefault(DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME,
			this::getDefaultTestableMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultTestFactoryMethodTimeout() {
		return parseOrDefault(DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultTestableMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultBeforeAllMethodTimeout() {
		return parseOrDefault(DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultLifecycleMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultBeforeEachMethodTimeout() {
		return parseOrDefault(DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultLifecycleMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultAfterEachMethodTimeout() {
		return parseOrDefault(DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultLifecycleMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultAfterAllMethodTimeout() {
		return parseOrDefault(DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultLifecycleMethodTimeout);
	}

	private Optional<TimeoutDuration> getDefaultTestableMethodTimeout() {
		return parseOrDefault(DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultTimeout);
	}

	private Optional<TimeoutDuration> getDefaultLifecycleMethodTimeout() {
		return parseOrDefault(DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultTimeout);
	}

	private Optional<TimeoutDuration> getDefaultTimeout() {
		return parseTimeoutDuration(DEFAULT_TIMEOUT_PROPERTY_NAME);
	}

	private Optional<TimeoutDuration> parseOrDefault(String propertyName,
			Supplier<Optional<TimeoutDuration>> defaultSupplier) {
		Optional<TimeoutDuration> timeoutConfiguration = parseTimeoutDuration(propertyName);
		return timeoutConfiguration.isPresent() ? timeoutConfiguration : defaultSupplier.get();
	}

	private Optional<TimeoutDuration> parseTimeoutDuration(String propertyName) {
		return cache.computeIfAbsent(propertyName, key -> extensionContext.getConfigurationParameter(key).map(value -> {
			try {
				return parser.parse(value);
			}
			catch (Exception e) {
				logger.warn(e,
					() -> "Ignored invalid timeout '%s' set via the '%s' configuration parameter.".formatted(value,
						key));
				return null;
			}
		}));
	}

	Optional<ThreadMode> getDefaultTimeoutThreadMode() {
		if (threadMode.get() != null) {
			return threadMode.get();
		}
		else {
			Optional<ThreadMode> configuredThreadMode = parseTimeoutThreadModeConfiguration();
			threadMode.set(configuredThreadMode);
			return configuredThreadMode;
		}
	}

	private Optional<ThreadMode> parseTimeoutThreadModeConfiguration() {
		return new EnumConfigurationParameterConverter<>(ThreadMode.class, "timeout thread mode") //
				.get(extensionContext, DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME);
	}

	private enum TimeoutMode {

		ENABLED {
			@Override
			boolean isTimeoutDisabled() {
				return false;
			}
		},

		DISABLED {
			@Override
			boolean isTimeoutDisabled() {
				return true;
			}
		},

		DISABLED_ON_DEBUG {
			@Override
			boolean isTimeoutDisabled() {
				return RuntimeUtils.isDebugMode();
			}
		};

		abstract boolean isTimeoutDisabled();
	}

}
