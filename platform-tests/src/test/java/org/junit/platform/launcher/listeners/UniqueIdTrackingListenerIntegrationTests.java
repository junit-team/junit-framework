/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.listeners;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.listeners.UniqueIdTrackingListener.DEFAULT_OUTPUT_FILE_PREFIX;
import static org.junit.platform.launcher.listeners.UniqueIdTrackingListener.LISTENER_ENABLED_PROPERTY_NAME;
import static org.junit.platform.launcher.listeners.UniqueIdTrackingListener.OUTPUT_DIR_PROPERTY_NAME;
import static org.junit.platform.launcher.listeners.UniqueIdTrackingListener.OUTPUT_FILE_PREFIX_PROPERTY_NAME;
import static org.junit.platform.launcher.listeners.UniqueIdTrackingListener.WORKING_DIR_PROPERTY_NAME;
import static org.junit.platform.testkit.engine.Event.byTestDescriptor;
import static org.junit.platform.testkit.engine.EventConditions.abortedWithReason;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

/**
 * Integration tests for the {@link UniqueIdTrackingListener}.
 *
 * @since 1.8
 */
class UniqueIdTrackingListenerIntegrationTests {

	private static final String passingTest = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase1]/[method:passingTest()]";
	private static final String skippedTest = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase1]/[method:skippedTest()]";
	private static final String abortedTest = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase1]/[method:abortedTest()]";
	private static final String failingTest = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase1]/[method:failingTest()]";
	private static final String dynamicTest1 = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase1]/[test-factory:dynamicTests()]/[dynamic-test:#1]";
	private static final String dynamicTest2 = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase1]/[test-factory:dynamicTests()]/[dynamic-test:#2]";
	private static final String testA = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase2]/[method:testA()]";
	private static final String testB = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase2]/[method:testB()]";
	private static final String testC = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase3]/[method:testC()]";
	private static final String testD = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase3]/[method:testD()]";
	private static final String testE = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase4]/[method:testE()]";
	private static final String testF = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$TestCase4]/[method:testF()]";
	private static final String testG = "[engine:junit-jupiter]/[class:org.junit.platform.launcher.listeners.UniqueIdTrackingListenerIntegrationTests$DisabledTestCase]/[nested-class:Inner]/[method:testG()]";

	private static final String[] expectedUniqueIds = { passingTest, skippedTest, abortedTest, failingTest,
			dynamicTest1, dynamicTest2, testA, testB, testG };

	private static final String[] expectedConcurrentUniqueIds = { testA, testB, testC, testD, testE, testF };

	@TempDir
	Path workingDir;

	@BeforeEach
	void createFakeGradleFiles() throws Exception {
		Files.createFile(workingDir.resolve("build.gradle"));
		Files.createDirectory(workingDir.resolve("build"));
	}

	@Test
	void confirmExpectedUniqueIdsViaEngineTestKit() {
		// @formatter:off
		EngineTestKit.engine("junit-jupiter")
			.selectors(selectClasses())
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats.started(7).skipped(1).aborted(1).succeeded(5).failed(1))
			.assertEventsMatchLoosely(
				event(test(uniqueId(passingTest)), finishedSuccessfully()),
				event(test(uniqueId(abortedTest)), abortedWithReason(instanceOf(TestAbortedException.class))),
				event(test(uniqueId(failingTest)), finishedWithFailure(instanceOf(AssertionFailedError.class))),
				event(test(uniqueId(dynamicTest1)), finishedSuccessfully()),
				event(test(uniqueId(dynamicTest2)), finishedSuccessfully()),
				event(test(uniqueId(testA)), finishedSuccessfully()),
				event(test(uniqueId(testB)), finishedSuccessfully())
			);
		// @formatter:on
	}

	private Condition<Event> uniqueId(String uniqueId) {
		return new Condition<>(
			byTestDescriptor(where(TestDescriptor::getUniqueId, uid -> uid.toString().equals(uniqueId))),
			"descriptor with uniqueId '%s'", uniqueId);
	}

	@Test
	void listenerIsRegisteredButDisabledByDefault() throws Exception {
		var numListenersRegistered = ServiceLoader.load(TestExecutionListener.class).stream()//
				.filter(provider -> UniqueIdTrackingListener.class.equals(provider.type()))//
				.count();
		assertThat(numListenersRegistered).isEqualTo(1);

		var actualUniqueIds = executeTests(Map.of());

		// Sanity check using the results of our local TestExecutionListener
		assertThat(actualUniqueIds).containsExactlyInAnyOrder(expectedUniqueIds);

		// Check that files were not generated by the UniqueIdTrackingListener
		assertThat(findFiles(workingDir, DEFAULT_OUTPUT_FILE_PREFIX)).isEmpty();
	}

	@Test
	void verifyUniqueIdsAreTrackedWithDefaults() throws Exception {
		verifyUniqueIdsAreTracked("build", DEFAULT_OUTPUT_FILE_PREFIX, Map.of());
	}

	@Test
	void verifyUniqueIdsAreTrackedWithCustomOutputFile() throws Exception {
		var customPrefix = "test_ids";
		verifyUniqueIdsAreTracked("build", customPrefix, Map.of(OUTPUT_FILE_PREFIX_PROPERTY_NAME, customPrefix));
	}

	@Test
	void verifyUniqueIdsAreTrackedWithCustomOutputDir() throws Exception {
		var customDir = "build/UniqueIdTrackingListenerIntegrationTests";
		verifyUniqueIdsAreTracked(customDir, DEFAULT_OUTPUT_FILE_PREFIX, Map.of(OUTPUT_DIR_PROPERTY_NAME, customDir));
	}

	@Test
	void verifyUniqueIdsAreTrackedWithCustomOutputFileAndCustomOutputDir() throws Exception {
		var customPrefix = "test_ids";
		var customDir = "build/UniqueIdTrackingListenerIntegrationTests";

		verifyUniqueIdsAreTracked(customDir, customPrefix,
			Map.of(OUTPUT_DIR_PROPERTY_NAME, customDir, OUTPUT_FILE_PREFIX_PROPERTY_NAME, customPrefix));
	}

	private void verifyUniqueIdsAreTracked(String outputDir, String prefix, Map<String, String> configurationParameters)
			throws IOException {

		configurationParameters = new HashMap<>(configurationParameters);
		configurationParameters.put(LISTENER_ENABLED_PROPERTY_NAME, "true");

		var actualUniqueIds = executeTests(configurationParameters);

		// Sanity check using the results of our local TestExecutionListener
		assertThat(actualUniqueIds).containsExactlyInAnyOrder(expectedUniqueIds);

		// Check contents of the file (or files) generated by the UniqueIdTrackingListener
		assertThat(readAllFiles(workingDir.resolve(outputDir), prefix)).containsExactlyInAnyOrder(expectedUniqueIds);
	}

	@Test
	void verifyUniqueIdsAreTrackedWithConcurrentlyExecutingTestPlans() throws Exception {
		var customDir = workingDir.resolve("build/UniqueIdTrackingListenerIntegrationTests");
		var prefix = DEFAULT_OUTPUT_FILE_PREFIX;

		Map<String, String> configurationParameters = new HashMap<>();
		configurationParameters.put(LISTENER_ENABLED_PROPERTY_NAME, "true");
		configurationParameters.put(OUTPUT_DIR_PROPERTY_NAME, customDir.toAbsolutePath().toString());

		Stream.of(TestCase2.class, TestCase3.class, TestCase4.class).parallel()//
				.forEach(clazz -> executeTests(configurationParameters, selectClass(clazz)));

		// 3 output files should have been generated.
		assertThat(findFiles(customDir, prefix)).hasSize(3);

		// Check contents of the file (or files) generated by the UniqueIdTrackingListener
		assertThat(readAllFiles(customDir, prefix)).containsExactlyInAnyOrder(expectedConcurrentUniqueIds);
	}

	private List<String> executeTests(Map<String, String> configurationParameters) {
		return executeTests(configurationParameters, selectClasses());
	}

	private List<String> executeTests(Map<String, String> configurationParameters, ClassSelector... classSelectors) {
		return executeTests(configurationParameters, List.of(classSelectors));
	}

	private List<String> executeTests(Map<String, String> configurationParameters, List<ClassSelector> classSelectors) {
		List<String> uniqueIds = new ArrayList<>();
		var listener = new TestExecutionListener() {

			@Nullable
			private TestPlan testPlan;

			@Override
			public void testPlanExecutionStarted(TestPlan testPlan) {
				this.testPlan = testPlan;
			}

			@Override
			public void executionSkipped(TestIdentifier testIdentifier, String reason) {
				if (testIdentifier.isTest()) {
					uniqueIds.add(testIdentifier.getUniqueId());
				}
				else {
					requireNonNull(this.testPlan).getChildren(testIdentifier).forEach(
						child -> executionSkipped(child, reason));
				}
			}

			@Override
			public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
				if (testIdentifier.isTest()) {
					uniqueIds.add(testIdentifier.getUniqueId());
				}
			}
		};
		var request = request()//
				.selectors(classSelectors)//
				.filters(includeEngines("junit-jupiter"))//
				.configurationParameters(configurationParameters)//
				.configurationParameter(WORKING_DIR_PROPERTY_NAME, workingDir.toAbsolutePath().toString())//
				.forExecution()//
				.listeners(listener)//
				.build();
		LauncherFactory.create().execute(request);
		return uniqueIds;
	}

	private static List<ClassSelector> selectClasses() {
		return DiscoverySelectors.selectClasses(TestCase1.class, TestCase2.class, DisabledTestCase.class);
	}

	private static Stream<Path> findFiles(Path outputDir, String prefix) throws IOException {
		if (!Files.exists(outputDir)) {
			return Stream.empty();
		}
		return Files.find(outputDir, 1, //
			(path, basicFileAttributes) -> (basicFileAttributes.isRegularFile()
					&& path.getFileName().toString().startsWith(prefix)));
	}

	private Stream<String> readAllFiles(Path outputDir, String prefix) throws IOException {
		return findFiles(outputDir, prefix).map(outputFile -> {
			try {
				return Files.readAllLines(outputFile);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}).flatMap(List::stream);
	}

	// -------------------------------------------------------------------------

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCase1 {

		@Test
		void passingTest() {
		}

		@Test
		@Disabled("testing")
		void skippedTest() {
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void abortedTest() {
			assumeTrue(false);
		}

		@Test
		void failingTest() {
			fail();
		}

		@TestFactory
		Stream<DynamicTest> dynamicTests() {
			return Stream.of("cat", "dog").map(text -> dynamicTest(text, () -> assertEquals(3, text.length())));
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCase2 {

		@Test
		void testA() {
		}

		@Test
		void testB() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCase3 {

		@Test
		void testC() {
		}

		@Test
		void testD() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	static class TestCase4 {

		@Test
		void testE() {
		}

		@Test
		void testF() {
		}
	}

	@Disabled
	static class DisabledTestCase {

		@Nested
		class Inner {

			@Test
			void testG() {
			}
		}
	}

}
