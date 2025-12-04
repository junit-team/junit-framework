/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.PrintWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;

/**
 * @since 5.0
 */
class UsingTheLauncherDemo {

	@Tag("exclude")
	@Test
	@SuppressWarnings("unused")
	void execution() {
		// @formatter:off
		// tag::execution[]
		LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
			.selectors(
				selectPackage("com.example.mytests"),
				selectClass(MyTestClass.class)
			)
			.filters(
				includeClassNamePatterns(".*Tests")
			)
			// end::execution[]
			.configurationParameter("enableHttpServer", "false")
			// tag::execution[]
			.build();

		SummaryGeneratingListener listener = new SummaryGeneratingListener();

		try (LauncherSession session = LauncherFactory.openSession()) {
			Launcher launcher = session.getLauncher();
			// Register one ore more listeners of your choice.
			launcher.registerTestExecutionListeners(listener);
			// Discover tests and build a test plan.
			TestPlan testPlan = launcher.discover(discoveryRequest);
			// Execute the test plan.
			launcher.execute(testPlan);
			// Alternatively, execute the discovery request directly.
			launcher.execute(discoveryRequest);
		}

		TestExecutionSummary summary = listener.getSummary();
		// Do something with the summary...

		// end::execution[]
		// @formatter:on
	}

	@Test
	void launcherConfig() {
		Path reportsDir = Path.of("target", "xml-reports");
		PrintWriter out = new PrintWriter(System.out);
		// @formatter:off
		// tag::launcherConfig[]
		LauncherConfig launcherConfig = LauncherConfig.builder()
			.enableTestEngineAutoRegistration(false)
			.enableLauncherSessionListenerAutoRegistration(false)
			.enableLauncherDiscoveryListenerAutoRegistration(false)
			.enablePostDiscoveryFilterAutoRegistration(false)
			.enableTestExecutionListenerAutoRegistration(false)
			.addTestEngines(new CustomTestEngine())
			.addLauncherSessionListeners(new CustomLauncherSessionListener())
			.addLauncherDiscoveryListeners(new CustomLauncherDiscoveryListener())
			.addPostDiscoveryFilters(new CustomPostDiscoveryFilter())
			.addTestExecutionListeners(new LegacyXmlReportGeneratingListener(reportsDir, out))
			.addTestExecutionListeners(new CustomTestExecutionListener())
			.build();

		LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request()
			.selectors(selectPackage("com.example.mytests"))
			.build();

		try (LauncherSession session = LauncherFactory.openSession(launcherConfig)) {
			session.getLauncher().execute(discoveryRequest);
		}
		// end::launcherConfig[]
		// @formatter:on
	}

	@Test
	@SuppressWarnings("unused")
	void cancellation() {
		// tag::cancellation[]
		CancellationToken cancellationToken = CancellationToken.create(); // <1>

		TestExecutionListener failFastListener = new TestExecutionListener() {
			@Override
			public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
				if (result.getStatus() == FAILED) {
					cancellationToken.cancel(); // <2>
				}
			}
		};

		// end::cancellation[]
		// @formatter:off
		// tag::cancellation[]
		LauncherExecutionRequest executionRequest = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectClass(MyTestClass.class))
				.forExecution()
				.cancellationToken(cancellationToken) // <3>
				.listeners(failFastListener) // <4>
				.build();
		// end::cancellation[]
		// @formatter:off
		// tag::cancellation[]

		try (LauncherSession session = LauncherFactory.openSession()) {
			session.getLauncher().execute(executionRequest); // <5>
		}
		// end::cancellation[]
		// @formatter:on
	}

}

class MyTestClass {
}

class CustomTestExecutionListener implements TestExecutionListener {
}

class CustomLauncherSessionListener implements LauncherSessionListener {
}

class CustomLauncherDiscoveryListener implements LauncherDiscoveryListener {
}

class CustomPostDiscoveryFilter implements PostDiscoveryFilter {
	@Override
	public FilterResult apply(TestDescriptor object) {
		return FilterResult.included("includes everything");
	}
}
