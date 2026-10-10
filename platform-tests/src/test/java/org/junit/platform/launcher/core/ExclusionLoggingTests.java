/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestDescriptor;

@Isolated("Changes global logging configuration")
class ExclusionLoggingTests {

	private final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(
		EngineDiscoveryOrchestrator.class.getName());
	private final List<LogRecord> records = new ArrayList<>();
	private final LogRecordListener listener = new LogRecordListener() {
		@Override
		public void logRecordSubmitted(LogRecord record) {
			records.add(record);
		}
	};

	private Level previousLevel;
	private boolean previousUseParentHandlers;
	private Method logExclusions;

	@BeforeEach
	void setUp() throws NoSuchMethodException {
		previousLevel = logger.getLevel();
		previousUseParentHandlers = logger.getUseParentHandlers();
		logger.setUseParentHandlers(false);
		logExclusions = EngineDiscoveryOrchestrator.class.getDeclaredMethod("logTestDescriptorExclusionReasons",
			Map.class);
		logExclusions.setAccessible(true);
	}

	@AfterEach
	void tearDown() {
		LoggerFactory.removeListener(listener);
		logger.setLevel(previousLevel);
		logger.setUseParentHandlers(previousUseParentHandlers);
	}

	@ParameterizedTest
	@EnumSource(LoggingMode.class)
	void computesOnlyConsumedMessagesAndPreservesTheirContents(LoggingMode mode) throws ReflectiveOperationException {
		configure(mode);
		var container = descriptor("container", true, false);
		var test = descriptor("test", false, true);
		var both = descriptor("both", true, true);
		var descriptors = List.of(container, test, both);

		logExclusions.invoke(new EngineDiscoveryOrchestrator(List.of(), List.of()), Map.of("filtered", descriptors));

		boolean countsConsumed = mode != LoggingMode.OFF;
		boolean namesConsumed = mode == LoggingMode.FINE || mode == LoggingMode.LISTENER;
		for (var descriptor : descriptors) {
			verify(descriptor, times(countsConsumed ? 1 : 0)).isContainer();
			verify(descriptor, times(countsConsumed ? 1 : 0)).isTest();
			verify(descriptor, times(namesConsumed ? 1 : 0)).getDisplayName();
		}
		// The test build bridges JUL to Log4j, so assert message contents via the
		// JUnit listener. The other modes verify which computations JUL requests.
		var expected = new ArrayList<String>();
		if (mode == LoggingMode.LISTENER) {
			expected.add("CONFIG: 2 containers and 2 tests were filtered");
		}
		if (mode == LoggingMode.LISTENER) {
			expected.add("FINE: The following containers and tests were filtered: container, test, both");
		}
		assertThat(
			records.stream().map(record -> record.getLevel() + ": " + record.getMessage())).containsExactlyElementsOf(
				expected);
	}

	@ParameterizedTest
	@EnumSource(LoggingMode.class)
	void doesNotLogWhenThereAreNoExclusions(LoggingMode mode) throws ReflectiveOperationException {
		configure(mode);

		logExclusions.invoke(new EngineDiscoveryOrchestrator(List.of(), List.of()), Map.of());

		assertThat(records).isEmpty();
	}

	private void configure(LoggingMode mode) {
		logger.setLevel(mode == LoggingMode.LISTENER ? Level.OFF : Level.parse(mode.name()));
		if (mode == LoggingMode.LISTENER) {
			LoggerFactory.addListener(listener);
		}
	}

	private TestDescriptor descriptor(String name, boolean container, boolean test) {
		var descriptor = mock(TestDescriptor.class);
		when(descriptor.getDisplayName()).thenReturn(name);
		when(descriptor.isContainer()).thenReturn(container);
		when(descriptor.isTest()).thenReturn(test);
		return descriptor;
	}

	enum LoggingMode {
		OFF, CONFIG, FINE, LISTENER
	}

}
