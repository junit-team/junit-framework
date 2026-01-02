/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

@ParameterizedClass
@EnumSource
record EngineDiscoveryResultsIntegrationTests(TestKitApi testKit) {

	@Test
	void returnsEngineDescriptor() {
		var results = testKit.discover("junit-jupiter", selectClass(TestCase.class));

		assertThat(results.getEngineDescriptor().getDisplayName()).isEqualTo("JUnit Jupiter");
		assertThat(getOnlyElement(results.getEngineDescriptor().getChildren()).getSource()) //
				.contains(ClassSource.from(TestCase.class));
	}

	@Test
	@NullMarked
	void collectsDiscoveryIssues() {
		var issue = DiscoveryIssue.create(Severity.WARNING, "warning");
		var testEngine = new TestEngineStub() {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, issue);
				return super.discover(discoveryRequest, uniqueId);
			}
		};

		var results = testKit.discover(testEngine);

		assertThat(results.getDiscoveryIssues()).containsExactly(issue);
	}

	static class TestCase {
		@Test
		void test() {
		}
	}

	enum TestKitApi {

		STATIC_METHOD {
			@Override
			EngineDiscoveryResults discover(String engineId, DiscoverySelector selector) {
				return EngineTestKit.discover(engineId, newRequest().selectors(selector).build());
			}

			@Override
			EngineDiscoveryResults discover(TestEngine testEngine) {
				return EngineTestKit.discover(testEngine, newRequest().build());
			}

			private static LauncherDiscoveryRequestBuilder newRequest() {
				return request().enableImplicitConfigurationParameters(false);
			}
		},

		FLUENT_API {
			@Override
			EngineDiscoveryResults discover(String engineId, DiscoverySelector selector) {
				return EngineTestKit.engine(engineId).selectors(selector).discover();
			}

			@Override
			EngineDiscoveryResults discover(TestEngine testEngine) {
				return EngineTestKit.engine(testEngine).discover();
			}
		};

		@SuppressWarnings("SameParameterValue")
		abstract EngineDiscoveryResults discover(String engineId, DiscoverySelector selector);

		abstract EngineDiscoveryResults discover(TestEngine testEngine);
	}
}
