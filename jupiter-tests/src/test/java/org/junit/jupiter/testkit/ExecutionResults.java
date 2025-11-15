/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.testkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

/**
 * Pioneers' class to handle JUnit Jupiter's {@link org.junit.platform.testkit.engine.EngineExecutionResults}.
 *
 * <p>Instantiate with the static factory methods in {@link JUnitJupiterTestKit}.
 */
public class ExecutionResults {

	private final EngineExecutionResults executionResults;

	private static final String JUPITER_ENGINE_NAME = "junit-jupiter";

	static class Builder {

		private final Map<String, String> additionalProperties = new HashMap<>();
		private final List<DiscoverySelector> selectors = new ArrayList<>();

		Builder addConfigurationParameters(Map<String, String> additionalConfig) {
			additionalProperties.putAll(additionalConfig);
			return this;
		}

		Executor selectTestClass(Class<?> testClass) {
			selectors.add(DiscoverySelectors.selectClass(testClass));
			return new Executor();
		}

		Executor selectTestClasses(Iterable<Class<?>> testClasses) {
			StreamSupport.stream(testClasses.spliterator(), false).map(DiscoverySelectors::selectClass).forEach(
				selectors::add);
			return new Executor();
		}

		Executor selectTestMethod(Class<?> testClass, String testMethodName) {
			selectors.add(DiscoverySelectors.selectMethod(testClass, testMethodName));
			return new Executor();
		}

		Executor selectTestMethodWithParameterTypes(Class<?> testClass, String testMethodName,
				String methodParameterTypes) {
			selectors.add(DiscoverySelectors.selectMethod(testClass, testMethodName, methodParameterTypes));
			return new Executor();
		}

		Executor selectNestedTestClass(List<Class<?>> enclosingClasses, Class<?> testClass) {
			selectors.add(DiscoverySelectors.selectNestedClass(enclosingClasses, testClass));
			return new Executor();
		}

		Executor selectNestedTestMethod(List<Class<?>> enclosingClasses, Class<?> testClass, String testMethodName) {
			selectors.add(DiscoverySelectors.selectNestedMethod(enclosingClasses, testClass, testMethodName));
			return new Executor();
		}

		Executor selectNestedTestMethodWithParameterTypes(List<Class<?>> enclosingClasses, Class<?> testClass,
				String testMethodName, String methodParameterTypes) {
			selectors.add(DiscoverySelectors.selectNestedMethod(enclosingClasses, testClass, testMethodName,
				methodParameterTypes));
			return new Executor();
		}

		class Executor {

			ExecutionResults execute() {
				return new ExecutionResults(Builder.this.additionalProperties, Builder.this.selectors);
			}

		}

	}

	private ExecutionResults(Map<String, String> additionalProperties, List<DiscoverySelector> selectors) {
		this.executionResults = getConfiguredJupiterEngine().configurationParameters(additionalProperties).selectors(
			selectors.toArray(DiscoverySelector[]::new)).execute();
	}

	static Builder builder() {
		return new Builder();
	}

	private EngineTestKit.Builder getConfiguredJupiterEngine() {
		return EngineTestKit.engine(JUPITER_ENGINE_NAME)
				// to tease out concurrency-related bugs, we want parallel execution of our tests
				// (for details, see section "Thread-safety" in CONTRIBUTING.adoc)
				.configurationParameter("junit.jupiter.execution.parallel.enabled", "true").configurationParameter(
					"junit.jupiter.execution.parallel.mode.default", "concurrent")
				// since we have full control over which tests we execute with this engine,
				// we can parallelize more aggressively than in the general settings in `junit-platform.properties`
				.configurationParameter("junit.jupiter.execution.parallel.mode.classes.default",
					"concurrent").configurationParameter("junit.jupiter.execution.parallel.config.strategy",
						"dynamic").configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor",
							"1");
	}

	/**
	 * Get all recorded events.
	 */
	public Events allEvents() {
		return executionResults.allEvents();
	}

	/**
	 * Get recorded dynamically registered events.
	 */
	public Events dynamicallyRegisteredEvents() {
		return executionResults.allEvents().dynamicallyRegistered();
	}

	/**
	 * Get recorded events for containers.
	 *
	 * <p>In this context, the word "container" applies to {@link org.junit.platform.engine.TestDescriptor
	 * TestDescriptors} that return {@code true} from {@link org.junit.platform.engine.TestDescriptor#isContainer()}.</p>
	 */
	public Events containerEvents() {
		return executionResults.containerEvents();
	}

	/**
	 * Get recorded events for tests.
	 *
	 * <p>In this context, the word "test" applies to {@link org.junit.platform.engine.TestDescriptor
	 * TestDescriptors} that return {@code true} from {@link org.junit.platform.engine.TestDescriptor#isTest()}.</p>
	 */
	public Events testEvents() {
		return executionResults.testEvents();
	}

}
