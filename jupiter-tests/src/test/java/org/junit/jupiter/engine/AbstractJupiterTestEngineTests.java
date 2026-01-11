/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static kotlin.jvm.JvmClassMappingKt.getJavaClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.STACKTRACE_PRUNING_ENABLED_PROPERTY_NAME;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.OutputDirectoryCreators.dummyOutputDirectoryCreator;

import java.util.List;
import java.util.function.Consumer;

import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.testkit.engine.EngineDiscoveryResults;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import kotlin.reflect.KClass;

/**
 * Abstract base class for tests involving the {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
public abstract class AbstractJupiterTestEngineTests {

	private final JupiterTestEngine engine = new JupiterTestEngine();

	protected EngineExecutionResults executeTestsForClass(KClass<?> testClass) {
		return executeTestsForClass(getJavaClass(testClass));
	}

	protected EngineExecutionResults executeTestsForClass(Class<?> testClass) {
		return executeTests(selectClass(testClass));
	}

	protected EngineExecutionResults executeTests(DiscoverySelector... selectors) {
		return executeTests(List.of(selectors));
	}

	protected EngineExecutionResults executeTests(List<? extends DiscoverySelector> selectors) {
		return executeTests(request -> request.selectors(selectors));
	}

	protected EngineExecutionResults executeTests(Consumer<LauncherDiscoveryRequestBuilder> configurer) {
		var builder = defaultRequest();
		configurer.accept(builder);
		return executeTests(builder);
	}

	protected EngineExecutionResults executeTests(LauncherDiscoveryRequestBuilder builder) {
		return executeTests(builder.build());
	}

	protected EngineExecutionResults executeTests(LauncherDiscoveryRequest request) {
		return EngineTestKit.execute(this.engine, request);
	}

	protected TestDescriptor discoverTestsWithoutIssues(LauncherDiscoveryRequest request) {
		var results = discoverTests(request);
		assertThat(results.getDiscoveryIssues()).isEmpty();
		return results.getEngineDescriptor();
	}

	protected EngineDiscoveryResults discoverTestsForClass(Class<?> testClass) {
		return discoverTests(selectClass(testClass));
	}

	protected EngineDiscoveryResults discoverTests(Consumer<LauncherDiscoveryRequestBuilder> configurer) {
		var builder = defaultRequest();
		configurer.accept(builder);
		return discoverTests(builder);
	}

	protected EngineDiscoveryResults discoverTests(DiscoverySelector... selectors) {
		return discoverTests(request -> request.selectors(selectors));
	}

	protected EngineDiscoveryResults discoverTests(LauncherDiscoveryRequestBuilder builder) {
		return discoverTests(builder.build());
	}

	protected EngineDiscoveryResults discoverTests(LauncherDiscoveryRequest request) {
		return EngineTestKit.discover(this.engine, request);
	}

	protected EngineTestKit.Builder jupiterTestEngine() {
		return EngineTestKit.engine(this.engine) //
				.outputDirectoryCreator(dummyOutputDirectoryCreator()) //
				.configurationParameter(STACKTRACE_PRUNING_ENABLED_PROPERTY_NAME, String.valueOf(false)) //
				.configurationParameter(CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, Severity.INFO.name()) //
				.enableImplicitConfigurationParameters(false);
	}

	protected static LauncherDiscoveryRequestBuilder defaultRequest() {
		return request() //
				.outputDirectoryCreator(dummyOutputDirectoryCreator()) //
				.configurationParameter(STACKTRACE_PRUNING_ENABLED_PROPERTY_NAME, String.valueOf(false)) //
				.configurationParameter(CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, Severity.INFO.name()) //
				.enableImplicitConfigurationParameters(false);
	}

	protected UniqueId discoverUniqueId(Class<?> clazz, String methodName) {
		var results = discoverTests(selectMethod(clazz, methodName));
		var engineDescriptor = results.getEngineDescriptor();
		var descendants = engineDescriptor.getDescendants();
		// @formatter:off
		var testDescriptor = descendants.stream()
				.skip(descendants.size() - 1)
				.findFirst()
				.orElseGet(() -> fail("no descendants"));
		// @formatter:on
		return testDescriptor.getUniqueId();
	}

}
