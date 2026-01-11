/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.jfr;

import static org.junit.platform.commons.util.ExceptionUtils.readStackTrace;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.moditect.jfrunit.ExpectedEvent.event;
import static org.moditect.jfrunit.JfrEventsAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.DisabledOnOpenJ9;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;

@JfrEventTest
@DisabledOnOpenJ9
public class FlightRecordingDiscoveryListenerIntegrationTests {

	public JfrEvents jfrEvents = new JfrEvents();

	@Test
	@EnableEvent("org.junit.*")
	void reportsEvents() {
		var source = ClassSource.from(FlightRecordingDiscoveryListenerIntegrationTests.class);
		var cause = new RuntimeException("boom");
		var issue = DiscoveryIssue.builder(Severity.WARNING, "some message") //
				.source(source) //
				.cause(cause) //
				.build();

		var testEngine = new TestEngineStub() {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				discoveryRequest.getDiscoveryListener().issueEncountered(uniqueId, issue);
				return super.discover(discoveryRequest, uniqueId);
			}
		};

		EngineTestKit.discover(testEngine, request() //
				.selectors(selectClass(FlightRecordingDiscoveryListenerIntegrationTests.class)) //
				.listeners(new FlightRecordingDiscoveryListener()) //
				.enableImplicitConfigurationParameters(false) //
				.build());

		jfrEvents.awaitEvents();

		assertThat(jfrEvents) //
				.contains(event("org.junit.LauncherDiscovery") //
						.with("selectors", 1) //
						.with("filters", 0)) //
				.contains(event("org.junit.EngineDiscovery") //
						.with("uniqueId", "[engine:TestEngineStub]")) //
				.contains(event("org.junit.DiscoveryIssue") //
						.with("engineId", "[engine:TestEngineStub]") //
						.with("severity", "WARNING") //
						.with("message", "some message") //
						.with("source", source.toString()) //
						.with("cause", readStackTrace(cause)));
	}
}
