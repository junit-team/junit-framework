/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.fakes.FaultyTestEngines.createEngineThatCannotResolveAnything;
import static org.junit.platform.fakes.FaultyTestEngines.createEngineThatFailsToResolveAnything;
import static org.junit.platform.launcher.LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.fakes.TestEngineStub;

@TrackLogRecords
class LoggingLauncherDiscoveryListenerTests {

	@Test
	void logsWarningOnUnresolvedUniqueIdSelectorWithEnginePrefix(LogRecordListener log) {
		var engine = createEngineThatCannotResolveAnything("some-engine");
		var request = request() //
				.configurationParameter(DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME, "execution") //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging") //
				.selectors(selectUniqueId(UniqueId.forEngine(engine.getId()))) //
				.enableImplicitConfigurationParameters(false) //
				.build();
		var launcher = createLauncher(engine);

		launcher.discover(request);

		assertThat(log.stream(LoggingLauncherDiscoveryListener.class, Level.WARNING)) //
				.extracting(LogRecord::getMessage) //
				.containsExactly(
					"UniqueIdSelector [uniqueId = [engine:some-engine]] could not be resolved by [engine:some-engine]");
	}

	@Test
	void logsDebugMessageOnUnresolvedUniqueIdSelectorWithoutEnginePrefix(LogRecordListener log) {
		var engine = createEngineThatCannotResolveAnything("some-engine");
		var request = request() //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging") //
				.selectors(selectUniqueId(UniqueId.forEngine("some-other-engine"))) //
				.enableImplicitConfigurationParameters(false) //
				.build();
		var launcher = createLauncher(engine);

		launcher.discover(request);

		assertThat(log.stream(LoggingLauncherDiscoveryListener.class, Level.FINE)) //
				.extracting(LogRecord::getMessage) //
				.containsExactly(
					"UniqueIdSelector [uniqueId = [engine:some-other-engine]] could not be resolved by [engine:some-engine]");
	}

	@Test
	void logsErrorOnSelectorResolutionFailure(LogRecordListener log) {
		var rootCause = new RuntimeException();
		var engine = createEngineThatFailsToResolveAnything("some-engine", rootCause);
		var request = request() //
				.configurationParameter(DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME, "execution") //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging") //
				.selectors(selectClass(Object.class)) //
				.enableImplicitConfigurationParameters(false) //
				.build();
		var launcher = createLauncher(engine);

		launcher.discover(request);

		assertThat(log.stream(LoggingLauncherDiscoveryListener.class, Level.SEVERE)) //
				.extracting(LogRecord::getMessage) //
				.containsExactly(
					"Resolution of ClassSelector [className = 'java.lang.Object', classLoader = null] by [engine:some-engine] failed");
	}

	@Test
	void logsErrorOnEngineDiscoveryFailure(LogRecordListener log) {
		var rootCause = new RuntimeException();
		var engine = new TestEngineStub("some-engine") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				throw rootCause;
			}
		};
		var request = request() //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging") //
				.selectors(selectUniqueId(UniqueId.forEngine(engine.getId()))) //
				.enableImplicitConfigurationParameters(false) //
				.build();
		var launcher = createLauncher(engine);

		launcher.discover(request);

		var logRecord = log.stream(LoggingLauncherDiscoveryListener.class, Level.SEVERE).findFirst().get();
		assertThat(logRecord.getMessage()).isEqualTo("TestEngine with ID 'some-engine' failed to discover tests");
		assertThat(logRecord.getThrown()).isSameAs(rootCause);
	}

	@Test
	void logsTraceMessageOnStartAndEnd(LogRecordListener log) {
		var engine = new TestEngineStub("some-engine");
		var request = request() //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging") //
				.selectors(selectUniqueId(UniqueId.forEngine(engine.getId()))) //
				.enableImplicitConfigurationParameters(false) //
				.build();
		var launcher = createLauncher(engine);

		launcher.discover(request);

		assertThat(log.stream(LoggingLauncherDiscoveryListener.class, Level.FINER)) //
				.extracting(LogRecord::getMessage) //
				.containsExactly( //
					"Test discovery started", //
					"Engine [engine:some-engine] has started discovering tests", //
					"Engine [engine:some-engine] has finished discovering tests", //
					"Test discovery finished");
	}

}
