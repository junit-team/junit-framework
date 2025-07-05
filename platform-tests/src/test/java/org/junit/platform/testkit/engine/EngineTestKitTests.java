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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.EngineExecutionOrchestrator;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

class EngineTestKitTests {

	private static final String KEY = EngineTestKitTests.class.getName();

	@BeforeEach
	void setSystemProperty() {
		System.setProperty(KEY, "from system property");
	}

	@AfterEach
	void resetSystemProperty() {
		System.clearProperty(KEY);
	}

	@Test
	void ignoresImplicitConfigurationParametersByDefault() {
		var value = executeExampleTestCaseAndCollectValue(builder -> builder);

		assertThat(value).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void verifyRequestLevelStoreIsUsedInExecution() {
		TestEngine testEngine = mock(TestEngine.class);
		when(testEngine.getId()).thenReturn("test-engine");

		LauncherDiscoveryRequest request = mock(LauncherDiscoveryRequest.class);
		when(request.getConfigurationParameters()).thenReturn(mock());
		when(request.getDiscoveryListener()).thenReturn(LauncherDiscoveryListener.NOOP);

		try (MockedConstruction<EngineExecutionOrchestrator> mockedConstruction = mockConstruction(
			EngineExecutionOrchestrator.class)) {
			EngineTestKit.execute(testEngine, request);
			assertThat(mockedConstruction.constructed()).isNotEmpty();

			EngineExecutionOrchestrator mockOrchestrator = mockedConstruction.constructed().getFirst();
			ArgumentCaptor<NamespacedHierarchicalStore<Namespace>> storeCaptor = forClass(
				NamespacedHierarchicalStore.class);

			verify(mockOrchestrator).execute(any(), any(), storeCaptor.capture(), any());
			assertNotNull(storeCaptor.getValue(), "Request level store should be passed to execute");
		}
	}

	@ParameterizedTest
	@CsvSource({ "true, from system property", "false," })
	void usesImplicitConfigurationParametersWhenEnabled(boolean enabled, String expectedValue) {
		var value = executeExampleTestCaseAndCollectValue(
			builder -> builder.enableImplicitConfigurationParameters(enabled));

		assertThat(value).isEqualTo(Optional.ofNullable(expectedValue));
	}

	private Optional<String> executeExampleTestCaseAndCollectValue(UnaryOperator<EngineTestKit.Builder> configuration) {
		return configuration.apply(EngineTestKit.engine("junit-jupiter")) //
				.selectors(selectClass(ExampleTestCase.class)) //
				.execute() //
				.allEvents() //
				.reportingEntryPublished() //
				.map(event -> event.getPayload(ReportEntry.class).orElseThrow()) //
				.map(ReportEntry::getKeyValuePairs) //
				.map(entries -> entries.get(KEY)) //
				.findFirst();
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class ExampleTestCase {

		@RegisterExtension
		BeforeEachCallback callback = context -> context.getConfigurationParameter(KEY) //
				.ifPresent(value -> context.publishReportEntry(KEY, value));

		@Test
		void test() {
		}

	}

}
