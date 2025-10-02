/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.Objects.requireNonNull;
import static java.util.function.UnaryOperator.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.SelectorResolutionResult.unresolved;
import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.fakes.FaultyTestEngines.createEngineThatCannotResolveAnything;
import static org.junit.platform.fakes.FaultyTestEngines.createEngineThatFailsToResolveAnything;
import static org.junit.platform.launcher.LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.DRY_RUN_PROPERTY_NAME;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.engine.CancellationToken;
import org.junit.platform.engine.DiscoveryIssue;
import org.junit.platform.engine.DiscoveryIssue.Severity;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestDescriptor;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestEngine;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.fakes.TestEngineSpy;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.EngineDiscoveryResult;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.PostDiscoveryFilterStub;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * @since 1.0
 */
class DefaultLauncherTests {

	private static final String FOO = DefaultLauncherTests.class.getSimpleName() + ".foo";
	private static final String BAR = DefaultLauncherTests.class.getSimpleName() + ".bar";

	private static final Runnable noOp = () -> {
	};

	@Test
	void constructLauncherWithoutAnyEngines() {
		var launcher = createLauncher();

		assertPreconditionViolationFor(() -> launcher.discover(request().build())).withMessageContaining(
			"Cannot create Launcher without at least one TestEngine");
	}

	@Test
	void constructLauncherWithMultipleTestEnginesWithDuplicateIds() {
		var launcher = createLauncher(new DemoHierarchicalTestEngine("dummy id"),
			new DemoHierarchicalTestEngine("dummy id"));

		var exception = assertThrows(JUnitException.class, () -> launcher.discover(request().build()));

		assertThat(exception).hasMessageContaining("multiple engines with the same ID");
	}

	@Test
	void discoverEmptyTestPlanWithEngineWithoutAnyTests() {
		var launcher = createLauncher(new DemoHierarchicalTestEngine());

		var testPlan = launcher.discover(request().build());

		assertThat(testPlan.getRoots()).hasSize(1);
	}

	@Test
	void discoverTestPlanForEngineThatReturnsNullForItsRootDescriptor() {
		TestEngine engine = new TestEngineStub("some-engine-id") {

			@SuppressWarnings("DataFlowIssue")
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				return null;
			}
		};

		var discoveryListener = mock(LauncherDiscoveryListener.class);
		var testPlan = createLauncher(engine).discover(request() //
				.listeners(discoveryListener) //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging") //
				.build());
		assertThat(testPlan.getRoots()).hasSize(1);
		assertDiscoveryFailed(engine, inOrder(discoveryListener), discoveryListener);
	}

	@ParameterizedTest
	@ValueSource(classes = { Error.class, RuntimeException.class })
	void discoverErrorTestDescriptorForEngineThatThrowsInDiscoveryPhase(Class<? extends Throwable> throwableClass) {
		TestEngine engine = new TestEngineStub("my-engine-id") {

			@SuppressWarnings("DataFlowIssue")
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				try {
					var constructor = throwableClass.getDeclaredConstructor(String.class);
					throw ExceptionUtils.throwAsUncheckedException(constructor.newInstance("ignored"));
				}
				catch (Exception ignored) {
					return null;
				}
			}
		};

		var launcher = createLauncher(engine);
		var discoveryListener = mock(LauncherDiscoveryListener.class);
		var request = request() //
				.listeners(discoveryListener) //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging") //
				.build();
		var testPlan = launcher.discover(request);

		assertThat(testPlan.getRoots()).hasSize(1);
		var engineIdentifier = getOnlyElement(testPlan.getRoots());
		assertThat(getOnlyElement(testPlan.getRoots()).getDisplayName()).isEqualTo("my-engine-id");

		InOrder inOrder = inOrder(discoveryListener);
		inOrder.verify(discoveryListener).launcherDiscoveryStarted(request);
		assertDiscoveryFailed(engine, inOrder, discoveryListener);
		inOrder.verify(discoveryListener).launcherDiscoveryFinished(request);

		var listener = mock(TestExecutionListener.class);
		var executionRequest = LauncherExecutionRequestBuilder.request(testPlan) //
				.listeners(listener) //
				.build();
		launcher.execute(executionRequest);

		var testExecutionResult = ArgumentCaptor.forClass(TestExecutionResult.class);
		verify(listener).executionStarted(engineIdentifier);
		verify(listener).executionFinished(eq(engineIdentifier), testExecutionResult.capture());
		assertThat(testExecutionResult.getValue().getThrowable()).isPresent();
		assertThat(testExecutionResult.getValue().getThrowable().get()) //
				.hasMessage("TestEngine with ID 'my-engine-id' failed to discover tests");
	}

	private void assertDiscoveryFailed(TestEngine testEngine, InOrder inOrder,
			LauncherDiscoveryListener discoveryListener) {
		var engineId = testEngine.getId();
		var failureCaptor = ArgumentCaptor.forClass(EngineDiscoveryResult.class);
		inOrder.verify(discoveryListener).engineDiscoveryStarted(UniqueId.forEngine(engineId));
		inOrder.verify(discoveryListener).engineDiscoveryFinished(eq(UniqueId.forEngine(engineId)),
			failureCaptor.capture());
		var result = failureCaptor.getValue();
		assertThat(result.getStatus()).isEqualTo(EngineDiscoveryResult.Status.FAILED);
		assertThat(result.getThrowable()).isPresent();
		assertThat(result.getThrowable().get()).hasMessage(
			"TestEngine with ID '" + engineId + "' failed to discover tests");
	}

	@Test
	void reportsEngineExecutionFailuresWithoutPriorEvents() {
		var rootCause = new RuntimeException("something went horribly wrong");
		var engine = new TestEngineStub() {
			@Override
			public void execute(ExecutionRequest request) {
				throw rootCause;
			}
		};

		var listener = mock(TestExecutionListener.class);
		createLauncher(engine).execute(request().forExecution().listeners(listener).build());

		var testExecutionResult = ArgumentCaptor.forClass(TestExecutionResult.class);
		verify(listener).executionStarted(any());
		verify(listener).executionFinished(any(), testExecutionResult.capture());
		assertThat(testExecutionResult.getValue().getThrowable()).isPresent();
		assertThat(testExecutionResult.getValue().getThrowable().get()) //
				.hasMessage("TestEngine with ID 'TestEngineStub' failed to execute tests") //
				.cause().isSameAs(rootCause);
	}

	@Test
	void reportsEngineExecutionFailuresForSkippedEngine() {
		var rootCause = new RuntimeException("something went horribly wrong");
		var engine = new TestEngineStub() {
			@Override
			public void execute(ExecutionRequest request) {
				var engineDescriptor = request.getRootTestDescriptor();
				request.getEngineExecutionListener().executionSkipped(engineDescriptor, "not today");
				throw rootCause;
			}
		};

		var listener = mock(TestExecutionListener.class);
		createLauncher(engine).execute(request().forExecution().listeners(listener).build());

		var testExecutionResult = ArgumentCaptor.forClass(TestExecutionResult.class);
		verify(listener).executionStarted(any());
		verify(listener).executionFinished(any(), testExecutionResult.capture());
		assertThat(testExecutionResult.getValue().getThrowable()).isPresent();
		assertThat(testExecutionResult.getValue().getThrowable().get()) //
				.hasMessage("TestEngine with ID 'TestEngineStub' failed to execute tests") //
				.cause().isSameAs(rootCause);
	}

	@Test
	void reportsEngineExecutionFailuresForStartedEngine() {
		var rootCause = new RuntimeException("something went horribly wrong");
		var engine = new TestEngineStub() {
			@Override
			public void execute(ExecutionRequest request) {
				var engineDescriptor = request.getRootTestDescriptor();
				request.getEngineExecutionListener().executionStarted(engineDescriptor);
				throw rootCause;
			}
		};

		var listener = mock(TestExecutionListener.class);
		createLauncher(engine).execute(request().forExecution().listeners(listener).build());

		var testExecutionResult = ArgumentCaptor.forClass(TestExecutionResult.class);
		verify(listener).executionStarted(any());
		verify(listener).executionFinished(any(), testExecutionResult.capture());
		assertThat(testExecutionResult.getValue().getThrowable()).isPresent();
		assertThat(testExecutionResult.getValue().getThrowable().get()) //
				.hasMessage("TestEngine with ID 'TestEngineStub' failed to execute tests") //
				.cause().isSameAs(rootCause);
	}

	@Test
	void reportsEngineExecutionFailuresForSuccessfullyFinishedEngine() {
		var rootCause = new RuntimeException("something went horribly wrong");
		var engine = new TestEngineStub() {
			@Override
			public void execute(ExecutionRequest request) {
				var engineDescriptor = request.getRootTestDescriptor();
				request.getEngineExecutionListener().executionStarted(engineDescriptor);
				request.getEngineExecutionListener().executionFinished(engineDescriptor, successful());
				throw rootCause;
			}
		};

		var listener = mock(TestExecutionListener.class);
		createLauncher(engine).execute(request().forExecution().listeners(listener).build());

		var testExecutionResult = ArgumentCaptor.forClass(TestExecutionResult.class);
		verify(listener).executionStarted(any());
		verify(listener).executionFinished(any(), testExecutionResult.capture());
		assertThat(testExecutionResult.getValue().getThrowable()).isPresent();
		assertThat(testExecutionResult.getValue().getThrowable().get()) //
				.hasMessage("TestEngine with ID 'TestEngineStub' failed to execute tests") //
				.cause().isSameAs(rootCause);
	}

	@Test
	void reportsEngineExecutionFailuresForFailedFinishedEngine() {
		var rootCause = new RuntimeException("something went horribly wrong");
		var originalFailure = new RuntimeException("suppressed");
		var engine = new TestEngineStub() {
			@Override
			public void execute(ExecutionRequest request) {
				var engineDescriptor = request.getRootTestDescriptor();
				var listener = request.getEngineExecutionListener();
				listener.executionStarted(engineDescriptor);
				listener.executionFinished(engineDescriptor, TestExecutionResult.failed(originalFailure));
				throw rootCause;
			}
		};

		var listener = mock(TestExecutionListener.class);
		createLauncher(engine).execute(request().forExecution().listeners(listener).build());

		var testExecutionResult = ArgumentCaptor.forClass(TestExecutionResult.class);
		verify(listener).executionStarted(any());
		verify(listener).executionFinished(any(), testExecutionResult.capture());
		assertThat(testExecutionResult.getValue().getThrowable()).isPresent();
		assertThat(testExecutionResult.getValue().getThrowable().get()) //
				.hasMessage("TestEngine with ID 'TestEngineStub' failed to execute tests") //
				.hasSuppressedException(originalFailure) //
				.cause().isSameAs(rootCause);
	}

	@Test
	void reportsSkippedEngines() {
		var engine = new TestEngineStub() {
			@Override
			public void execute(ExecutionRequest request) {
				var engineDescriptor = request.getRootTestDescriptor();
				request.getEngineExecutionListener().executionSkipped(engineDescriptor, "not today");
			}
		};

		var listener = mock(TestExecutionListener.class);
		createLauncher(engine).execute(request().forExecution().listeners(listener).build());

		verify(listener).executionSkipped(any(TestIdentifier.class), eq("not today"));
		verify(listener, times(0)).executionStarted(any());
		verify(listener, times(0)).executionFinished(any(), any());
	}

	@Test
	void reportsFinishedEngines() {
		var engine = new TestEngineStub() {
			@Override
			public void execute(ExecutionRequest request) {
				var engineDescriptor = request.getRootTestDescriptor();
				var listener = request.getEngineExecutionListener();
				listener.executionStarted(engineDescriptor);
				listener.executionFinished(engineDescriptor, successful());
			}
		};

		var listener = mock(TestExecutionListener.class);
		createLauncher(engine).execute(request().forExecution().listeners(listener).build());

		verify(listener).executionStarted(any());
		verify(listener).executionFinished(any(), eq(successful()));
	}

	@Test
	void discoverTestPlanForSingleEngine() {
		var engine = new DemoHierarchicalTestEngine("myEngine");
		engine.addTest("test1", noOp);
		engine.addTest("test2", noOp);

		var launcher = createLauncher(engine);

		var testPlan = launcher.discover(request().selectors(selectPackage("any")).build());

		assertThat(testPlan.getRoots()).hasSize(1);
		var rootIdentifier = testPlan.getRoots().iterator().next();
		assertThat(testPlan.getChildren(rootIdentifier.getUniqueIdObject())).hasSize(2);
		assertThat(testPlan.getChildren(UniqueId.parse("[engine:myEngine]"))).hasSize(2);
	}

	@Test
	void discoverTestPlanForMultipleEngines() {
		var firstEngine = new DemoHierarchicalTestEngine("engine1");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		var secondEngine = new DemoHierarchicalTestEngine("engine2");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		var launcher = createLauncher(firstEngine, secondEngine);

		var testPlan = launcher.discover(
			request().selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId())).build());

		assertThat(testPlan.getRoots()).hasSize(2);
		assertThat(testPlan.getChildren(UniqueId.forEngine("engine1"))).hasSize(1);
		assertThat(testPlan.getChildren(UniqueId.forEngine("engine2"))).hasSize(1);
	}

	@Test
	void launcherAppliesPostDiscoveryFilters() {
		var engine = new DemoHierarchicalTestEngine("myEngine");
		var test1 = engine.addTest("test1", noOp);
		engine.addTest("test2", noOp);

		var launcher = createLauncher(engine);

		PostDiscoveryFilter includeWithUniqueIdContainsTest = new PostDiscoveryFilterStub(
			descriptor -> FilterResult.includedIf(descriptor.getUniqueId().toString().contains("test")),
			() -> "filter1");
		PostDiscoveryFilter includeWithUniqueIdContains1 = new PostDiscoveryFilterStub(
			descriptor -> FilterResult.includedIf(descriptor.getUniqueId().toString().contains("1")), () -> "filter2");

		var testPlan = launcher.discover( //
			request() //
					.selectors(selectPackage("any")) //
					.filters(includeWithUniqueIdContainsTest, includeWithUniqueIdContains1) //
					.build());

		assertThat(testPlan.getChildren(UniqueId.forEngine("myEngine"))).hasSize(1);
		assertThat(testPlan.getTestIdentifier(test1.getUniqueId())).isNotNull();
	}

	@Test
	void withoutConfigurationParameters_LauncherPassesEmptyConfigurationParametersIntoTheExecutionRequest() {
		var engine = new TestEngineSpy();

		var launcher = createLauncher(engine);
		launcher.execute(request().forExecution().build());

		var configurationParameters = requireNonNull(engine.requestForExecution).getConfigurationParameters();
		assertThat(configurationParameters.get("key")).isNotPresent();
	}

	@Test
	void withConfigurationParameters_LauncherPassesPopulatedConfigurationParametersIntoTheExecutionRequest() {
		var engine = new TestEngineSpy();

		var launcher = createLauncher(engine);
		launcher.execute(request().configurationParameter("key", "value").forExecution().build());

		var configurationParameters = requireNonNull(engine.requestForExecution).getConfigurationParameters();
		assertThat(configurationParameters.get("key")).isPresent();
		assertThat(configurationParameters.get("key")).contains("value");
	}

	@Test
	void withoutConfigurationParameters_LookupFallsBackToSystemProperty() {
		System.setProperty(FOO, BAR);

		try {
			var engine = new TestEngineSpy();

			var launcher = createLauncher(engine);
			launcher.execute(request().forExecution().build());

			var configurationParameters = requireNonNull(engine.requestForExecution).getConfigurationParameters();
			var optionalFoo = configurationParameters.get(FOO);
			assertTrue(optionalFoo.isPresent(), "foo should have been picked up via system property");
			assertEquals(BAR, optionalFoo.get(), "foo property");
		}
		finally {
			System.clearProperty(FOO);
		}
	}

	@Test
	void withAdditionalListener() {
		var engine = new TestEngineSpy();
		var listener = new SummaryGeneratingListener();

		var launcher = createLauncher(engine);
		launcher.execute(request().forExecution().listeners(listener).build());

		assertThat(listener.getSummary()).isNotNull();
		assertThat(listener.getSummary().getContainersFoundCount()).isEqualTo(1);
		assertThat(listener.getSummary().getTestsFoundCount()).isEqualTo(1);
	}

	@Test
	void prunesTestDescriptorsAfterApplyingPostDiscoveryFilters() {
		var engine = new TestEngineSpy() {

			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				super.discover(discoveryRequest, uniqueId);
				var engineDescriptor = new TestDescriptorStub(uniqueId, uniqueId.toString());
				var containerDescriptor = new TestDescriptorStub(uniqueId.append("container", "a"), "container") {

					@Override
					public Type getType() {
						return Type.CONTAINER;
					}
				};
				containerDescriptor.addChild(
					new TestDescriptorStub(containerDescriptor.getUniqueId().append("test", "b"), "test"));
				engineDescriptor.addChild(containerDescriptor);
				return engineDescriptor;
			}
		};

		var launcher = createLauncher(engine);
		var testPlan = launcher.discover(request().filters(
			(PostDiscoveryFilter) testDescriptor -> FilterResult.includedIf(testDescriptor.isContainer())).build());

		assertThat(testPlan.getRoots()).hasSize(1);
		var engineIdentifier = getOnlyElement(testPlan.getRoots());
		assertThat(testPlan.getChildren(engineIdentifier)).isEmpty();
	}

	@Test
	void reportsDynamicTestDescriptorsCorrectly() {
		var engineId = UniqueId.forEngine("engine");
		var containerAndTestId = engineId.append("c&t", "c&t");
		var dynamicTestId = containerAndTestId.append("test", "test");

		var engine = new TestEngineSpy(engineId.getLastSegment().getValue()) {

			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				super.discover(discoveryRequest, uniqueId);
				var engineDescriptor = new TestDescriptorStub(uniqueId, uniqueId.toString());
				engineDescriptor.addChild(new TestDescriptorStub(containerAndTestId, "c&t") {

					@Override
					public Type getType() {
						return Type.CONTAINER_AND_TEST;
					}
				});
				return engineDescriptor;
			}

			@Override
			public void execute(ExecutionRequest request) {
				super.execute(request);
				var listener = request.getEngineExecutionListener();

				listener.executionStarted(request.getRootTestDescriptor());
				var containerAndTest = getOnlyElement(request.getRootTestDescriptor().getChildren());
				listener.executionStarted(containerAndTest);

				var dynamicTest = new TestDescriptorStub(dynamicTestId, "test");
				dynamicTest.setParent(containerAndTest);
				listener.dynamicTestRegistered(dynamicTest);
				listener.executionStarted(dynamicTest);
				listener.executionFinished(dynamicTest, successful());

				listener.executionFinished(containerAndTest, successful());
				listener.executionFinished(request.getRootTestDescriptor(), successful());
			}
		};

		var launcher = createLauncher(engine);
		var listener = mock(TestExecutionListener.class);
		launcher.execute(request().forExecution().listeners(listener).build());

		var inOrder = inOrder(listener);
		var testPlanArgumentCaptor = ArgumentCaptor.forClass(TestPlan.class);
		inOrder.verify(listener).testPlanExecutionStarted(testPlanArgumentCaptor.capture());

		var testPlan = testPlanArgumentCaptor.getValue();
		var engineTestIdentifier = testPlan.getTestIdentifier(engineId);
		var containerAndTestIdentifier = testPlan.getTestIdentifier(containerAndTestId);
		var dynamicTestIdentifier = testPlan.getTestIdentifier(dynamicTestId);
		assertThat(engineTestIdentifier.getParentIdObject()).isEmpty();
		assertThat(containerAndTestIdentifier.getParentIdObject()).contains(engineId);
		assertThat(dynamicTestIdentifier.getParentIdObject()).contains(containerAndTestId);

		inOrder.verify(listener).executionStarted(engineTestIdentifier);
		inOrder.verify(listener).executionStarted(containerAndTestIdentifier);
		inOrder.verify(listener).dynamicTestRegistered(dynamicTestIdentifier);
		inOrder.verify(listener).executionStarted(dynamicTestIdentifier);
		inOrder.verify(listener).executionFinished(dynamicTestIdentifier, successful());
		inOrder.verify(listener).executionFinished(containerAndTestIdentifier, successful());
		inOrder.verify(listener).executionFinished(engineTestIdentifier, successful());
		inOrder.verify(listener).testPlanExecutionFinished(same(testPlan));
	}

	@Test
	void launcherCanExecuteTestPlanExactlyOnce() {
		var engine = mock(TestEngine.class);
		when(engine.getId()).thenReturn("some-engine");
		when(engine.discover(any(), any())).thenAnswer(invocation -> {
			UniqueId uniqueId = invocation.getArgument(1);
			return new EngineDescriptor(uniqueId, uniqueId.toString());
		});

		var launcher = createLauncher(engine);
		var testPlan = launcher.discover(request().build());
		verify(engine, times(1)).discover(any(), any());

		var executionRequest = LauncherExecutionRequestBuilder.request(testPlan).build();
		launcher.execute(executionRequest);
		verify(engine, times(1)).execute(any());

		assertPreconditionViolationFor(() -> launcher.execute(executionRequest)).withMessage(
			"TestPlan must only be executed once");
	}

	@Test
	void thirdPartyEngineUsingReservedEngineIdPrefixEmitsWarning(@TrackLogRecords LogRecordListener listener) {
		var id = "junit-using-reserved-prefix";
		var launcher = createLauncher(new TestEngineStub(id));
		launcher.discover(request().build());
		assertThat(listener.stream(EngineIdValidator.class, Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly(
					"Third-party TestEngine implementations are forbidden to use the reserved 'junit-' prefix for their ID: '"
							+ id + "'");
	}

	@Test
	void thirdPartyEngineClaimingToBeJupiterResultsInException() {
		assertImposter("junit-jupiter");
	}

	@Test
	void thirdPartyEngineClaimingToBeVintageResultsInException() {
		assertImposter("junit-vintage");
	}

	private void assertImposter(String id) {
		TestEngine impostor = new TestEngineStub(id);
		var launcher = createLauncher(impostor);
		Exception exception = assertThrows(JUnitException.class, () -> launcher.discover(request().build()));
		assertThat(exception).hasMessage(
			"Third-party TestEngine '%s' is forbidden to use the reserved '%s' TestEngine ID.",
			impostor.getClass().getName(), id);
	}

	@Test
	void dryRunModeReportsEventsForAllTestsButDoesNotExecuteThem() {
		var engine = new DemoHierarchicalTestEngine("engine");
		var container = engine.addContainer("container", "Container", null);
		var test = new DemoHierarchicalTestDescriptor(container.getUniqueId().append("test", "test"), "Test",
			(__, ___) -> {
				throw new RuntimeException("boom");
			});
		container.addChild(test);

		var launcher = createLauncher(engine);
		TestExecutionListener listener = mock();

		var request = request() //
				.configurationParameter(DRY_RUN_PROPERTY_NAME, "true") //
				.forExecution() //
				.listeners(listener) //
				.build();
		launcher.execute(request);

		var inOrder = inOrder(listener);
		inOrder.verify(listener).testPlanExecutionStarted(any());
		inOrder.verify(listener).executionStarted(TestIdentifier.from(engine.getEngineDescriptor()));
		inOrder.verify(listener).executionStarted(TestIdentifier.from(container));
		inOrder.verify(listener).executionSkipped(TestIdentifier.from(test), "JUnit Platform dry-run mode is enabled");
		inOrder.verify(listener).executionFinished(TestIdentifier.from(container), successful());
		inOrder.verify(listener).executionFinished(TestIdentifier.from(engine.getEngineDescriptor()), successful());
		inOrder.verify(listener).testPlanExecutionFinished(any());
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void notifiesDiscoveryListenersOfProcessedSelectors() {
		TestEngine engine = new TestEngineStub("some-engine-id") {

			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				discoveryRequest.getSelectorsByType(DiscoverySelector.class).forEach(selector -> {
					discoveryRequest.getDiscoveryListener().selectorProcessed(uniqueId, selector, unresolved());
				});
				return new EngineDescriptor(uniqueId, uniqueId.getLastSegment().getValue());
			}
		};
		var engineId = UniqueId.forEngine(engine.getId());

		var discoveryListenerOnConfig = mock(LauncherDiscoveryListener.class, "discoveryListenerOnConfig");
		var discoveryListenerOnLauncher = mock(LauncherDiscoveryListener.class, "discoveryListenerOnLauncher");
		var discoveryListenerOnRequest = mock(LauncherDiscoveryListener.class, "discoveryListenerOnRequest");
		var selector = mock(DiscoverySelector.class);

		var launcherConfig = LauncherFactoryForTestingPurposesOnly.createLauncherConfigBuilderWithDisabledServiceLoading() //
				.addTestEngines(engine) //
				.addLauncherDiscoveryListeners(discoveryListenerOnConfig) //
				.build();

		var launcher = LauncherFactory.create(launcherConfig);
		launcher.registerLauncherDiscoveryListeners(discoveryListenerOnLauncher);

		launcher.discover(request() //
				.selectors(selector) //
				.listeners(discoveryListenerOnRequest) //
				.build());

		assertAll( //
			() -> verify(discoveryListenerOnConfig).selectorProcessed(engineId, selector, unresolved()), //
			() -> verify(discoveryListenerOnLauncher).selectorProcessed(engineId, selector, unresolved()), //
			() -> verify(discoveryListenerOnRequest).selectorProcessed(engineId, selector, unresolved()) //
		);
	}

	@Test
	void reportsEngineExecutionFailureForCriticalDiscoveryIssuesAndLogsRemaining(
			@TrackLogRecords LogRecordListener listener) {

		var result = execute(new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.ERROR, "error"));
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.WARNING, "warning"));
				return new EngineDescriptor(uniqueId, "Engine") {
					@Override
					public Set<TestTag> getTags() {
						return Set.of(TestTag.create("custom-tag"));
					}
				};
			}
		});

		assertThat(result.testPlan().containsTests()).isTrue();

		assertThat(result.testIdentifier().getDisplayName()).isEqualTo("Engine");
		assertThat(result.testIdentifier().getTags()).containsExactly(TestTag.create("custom-tag"));

		assertThat(result.testExecutionResult().getStatus()).isEqualTo(Status.FAILED);
		assertThat(result.testExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(DiscoveryIssueException.class) //
				.hasMessageStartingWith(
					"TestEngine with ID 'engine-id' encountered a critical issue during test discovery") //
				.hasMessageContaining("(1) [ERROR] error");

		var logRecord = findFirstDiscoveryIssueLogRecord(listener, Level.WARNING);
		assertThat(logRecord.getMessage()) //
				.startsWith("TestEngine with ID 'engine-id' encountered a non-critical issue during test discovery") //
				.contains("(1) [WARNING] warning");
		assertThat(logRecord.getInstant()) //
				.isBetween(result.startTime(), result.finishTime());
	}

	@Test
	void logsNonCriticalIssuesForRegularEngineExecution(@TrackLogRecords LogRecordListener listener) {

		var result = execute(new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.INFO, "info"));
				return new EngineDescriptor(uniqueId, "Engine");
			}

			@Override
			public void execute(ExecutionRequest request) {
				var executionListener = request.getEngineExecutionListener();
				var engineDescriptor = request.getRootTestDescriptor();
				executionListener.executionStarted(engineDescriptor);
				executionListener.executionFinished(engineDescriptor, successful());
			}
		});

		assertThat(result.testIdentifier().getDisplayName()).isEqualTo("Engine");
		assertThat(result.testExecutionResult().getStatus()).isEqualTo(Status.SUCCESSFUL);

		var logRecord = findFirstDiscoveryIssueLogRecord(listener, Level.INFO);
		assertThat(logRecord.getMessage()) //
				.startsWith("TestEngine with ID 'engine-id' encountered a non-critical issue during test discovery") //
				.contains("(1) [INFO] info");
		assertThat(logRecord.getInstant()) //
				.isBetween(result.startTime(), result.finishTime());
	}

	@Test
	void logsAllIssuesForDiscoveryFailure(@TrackLogRecords LogRecordListener listener) {

		var result = execute(new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.ERROR, "error"));
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.INFO, "info"));
				throw new RuntimeException("boom");
			}
		});

		assertThat(result.testPlan().containsTests()).isTrue();

		assertThat(result.testIdentifier().getDisplayName()).isEqualTo("engine-id");
		assertThat(result.testExecutionResult().getStatus()).isEqualTo(Status.FAILED);
		assertThat(result.testExecutionResult().getThrowable().orElseThrow()) //
				.hasMessage("TestEngine with ID 'engine-id' failed to discover tests") //
				.cause().hasMessage("boom");

		var logRecord = findFirstDiscoveryIssueLogRecord(listener, Level.SEVERE);
		assertThat(logRecord.getMessage()) //
				.startsWith("TestEngine with ID 'engine-id' encountered a critical issue during test discovery") //
				.contains("(1) [ERROR] error");
		assertThat(logRecord.getInstant()) //
				.isBetween(result.startTime(), result.finishTime());

		logRecord = findFirstDiscoveryIssueLogRecord(listener, Level.INFO);
		assertThat(logRecord.getMessage()) //
				.startsWith("TestEngine with ID 'engine-id' encountered a non-critical issue during test discovery") //
				.contains("(1) [INFO] info");
		assertThat(logRecord.getInstant()) //
				.isBetween(result.startTime(), result.finishTime());
	}

	@Test
	void logsNonCriticalIssuesForExecutionFailure(@TrackLogRecords LogRecordListener listener) {

		var result = execute(new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.INFO, "info"));
				return new EngineDescriptor(uniqueId, "Engine");
			}

			@Override
			public void execute(ExecutionRequest request) {
				throw new RuntimeException("boom");
			}
		});

		assertThat(result.testIdentifier().getDisplayName()).isEqualTo("Engine");

		assertThat(result.testExecutionResult().getThrowable().orElseThrow()) //
				.hasMessage("TestEngine with ID 'engine-id' failed to execute tests") //
				.cause().hasMessage("boom");

		var logRecord = findFirstDiscoveryIssueLogRecord(listener, Level.INFO);
		assertThat(logRecord.getMessage()) //
				.startsWith("TestEngine with ID 'engine-id' encountered a non-critical issue during test discovery") //
				.contains("(1) [INFO] info");
		assertThat(logRecord.getInstant()) //
				.isBetween(result.startTime(), result.finishTime());
	}

	@Test
	void reportsEngineExecutionFailureOnUnresolvedUniqueIdSelectorWithEnginePrefix() {
		var engine = createEngineThatCannotResolveAnything("some-engine");
		var selector = selectUniqueId(UniqueId.forEngine(engine.getId()));
		var result = execute(engine, request -> request.selectors(selector));

		assertThat(result.testExecutionResult().getStatus()).isEqualTo(Status.FAILED);
		assertThat(result.testExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(DiscoveryIssueException.class) //
				.hasMessageStartingWith(
					"TestEngine with ID 'some-engine' encountered a critical issue during test discovery") //
				.hasMessageContaining("(1) [ERROR] %s could not be resolved", selector);
	}

	@Test
	void ignoresUnresolvedUniqueIdSelectorWithoutEnginePrefix() {
		var engine = createEngineThatCannotResolveAnything("some-engine");
		var selector = selectUniqueId(UniqueId.forEngine("some-other-engine"));
		var result = execute(engine, request -> request.selectors(selector));

		assertThat(result.testExecutionResult().getStatus()).isEqualTo(Status.SUCCESSFUL);
	}

	@Test
	void reportsEngineExecutionFailureForSelectorResolutionFailure() {
		var engine = createEngineThatFailsToResolveAnything("some-engine", new RuntimeException("boom"));
		var selector = selectClass(Object.class);
		var result = execute(engine, request -> request.selectors(selector));

		assertThat(result.testExecutionResult().getStatus()).isEqualTo(Status.FAILED);
		assertThat(result.testExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(DiscoveryIssueException.class) //
				.hasMessageStartingWith(
					"TestEngine with ID 'some-engine' encountered a critical issue during test discovery") //
				.hasMessageContaining("(1) [ERROR] %s resolution failed", selector) //
				.hasMessageContaining("Cause: java.lang.RuntimeException: boom");
	}

	@Test
	void allowsConfiguringCriticalDiscoveryIssueSeverity() {

		var engine = new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.INFO, "info"));
				return new EngineDescriptor(uniqueId, "Engine");
			}
		};

		var result = execute(engine, request -> request //
				.configurationParameter(LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, "info"));

		assertThat(result.testExecutionResult().getStatus()).isEqualTo(Status.FAILED);
		assertThat(result.testExecutionResult().getThrowable().orElseThrow()) //
				.isInstanceOf(DiscoveryIssueException.class) //
				.hasMessageStartingWith(
					"TestEngine with ID 'engine-id' encountered a critical issue during test discovery") //
				.hasMessageContaining("(1) [INFO] info");
	}

	@Test
	void failsIfCriticalSeverityIsConfiguredIncorrectly() {

		var engine = new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.INFO, "info"));
				return new EngineDescriptor(uniqueId, "Engine");
			}

			@Override
			public void execute(ExecutionRequest request) {
				var executionListener = request.getEngineExecutionListener();
				var engineDescriptor = request.getRootTestDescriptor();
				executionListener.executionStarted(engineDescriptor);
				executionListener.executionFinished(engineDescriptor, successful());
			}
		};

		var exception = assertThrows(JUnitException.class, () -> execute(engine, request -> request //
				.configurationParameter(LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME, "wrong")));

		assertThat(exception) //
				.hasRootCauseMessage(
					"Invalid DiscoveryIssue.Severity 'wrong' set via the '%s' configuration parameter.",
					LauncherConstants.CRITICAL_DISCOVERY_ISSUE_SEVERITY_PROPERTY_NAME);
	}

	@Test
	void failsDuringDiscoveryIfConfigurationParameterIsSetAccordingly() {

		var engine = new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.ERROR, "error"));
				return new EngineDescriptor(uniqueId, "Engine");
			}
		};

		var exception = assertThrows(DiscoveryIssueException.class, () -> execute(engine, request -> request //
				.configurationParameter(LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME, "discovery")));

		assertThat(exception) //
				.isInstanceOf(DiscoveryIssueException.class) //
				.hasMessageStartingWith(
					"TestEngine with ID 'engine-id' encountered a critical issue during test discovery") //
				.hasMessageContaining("(1) [ERROR] error");
	}

	@ParameterizedTest
	@ValueSource(strings = { "discovery", "execution" })
	void logsNonCriticalIssuesOnlyOnce(String phase, @TrackLogRecords LogRecordListener listener) {

		var engine = new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var listener = discoveryRequest.getDiscoveryListener();
				listener.issueEncountered(uniqueId, DiscoveryIssue.create(Severity.WARNING, "warning"));
				return new EngineDescriptor(uniqueId, "Engine");
			}

			@Override
			public void execute(ExecutionRequest request) {
				var executionListener = request.getEngineExecutionListener();
				var engineDescriptor = request.getRootTestDescriptor();
				executionListener.executionStarted(engineDescriptor);
				executionListener.executionFinished(engineDescriptor, successful());
			}
		};

		execute(engine, request -> request //
				.configurationParameter(LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME, phase));

		assertThat(listener.stream(DiscoveryIssueNotifier.class, Level.WARNING)).hasSize(1);
	}

	@Test
	void failsDuringDiscoveryIfConfigurationParameterValueIsInvalid() {

		var engine = new TestEngineStub("engine-id");

		var exception = assertThrows(JUnitException.class, () -> execute(engine, request -> request //
				.configurationParameter(LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME, "wrong")));

		assertThat(exception) //
				.hasRootCauseMessage("Invalid LauncherPhase 'wrong' set via the '%s' configuration parameter.",
					LauncherConstants.DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME);
	}

	@Test
	void reportsChildrenOfEngineDescriptorAsSkippedAfterCancellationWasRequested() {
		var engine = spy(new TestEngineStub("engine-id") {
			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				var engineDescriptor = new EngineDescriptor(uniqueId, "Engine");
				var container = new TestDescriptorStub(uniqueId.append("container", "1"), "Container");
				var test = new TestDescriptorStub(container.getUniqueId().append("test", "1"), "Test");
				container.addChild(test);
				engineDescriptor.addChild(container);
				return engineDescriptor;
			}
		});

		var executionListener = mock(TestExecutionListener.class);

		var cancellationToken = CancellationToken.create();
		cancellationToken.cancel();

		execute(engine, identity(), executionRequest -> executionRequest //
				.listeners(executionListener) //
				.cancellationToken(cancellationToken));

		verify(engine, never()).execute(any());

		var inOrder = inOrder(executionListener);
		inOrder.verify(executionListener).testPlanExecutionStarted(any());
		inOrder.verify(executionListener).executionStarted(
			argThat(d -> d.getUniqueIdObject().equals(UniqueId.forEngine("engine-id"))));
		inOrder.verify(executionListener).executionSkipped(
			argThat(d -> d.getUniqueIdObject().getLastSegment().getType().equals("container")),
			eq("Execution cancelled"));
		inOrder.verify(executionListener).executionFinished(
			argThat(d -> d.getUniqueIdObject().equals(UniqueId.forEngine("engine-id"))),
			argThat(result -> result.getStatus() == ABORTED));
		inOrder.verify(executionListener).testPlanExecutionFinished(any());
		inOrder.verifyNoMoreInteractions();
	}

	private static ReportedData execute(TestEngine engine) {
		return execute(engine, identity());
	}

	private static ReportedData execute(TestEngine engine,
			UnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer) {
		return execute(engine, discoveryConfigurer, identity());
	}

	private static ReportedData execute(TestEngine engine,
			UnaryOperator<LauncherDiscoveryRequestBuilder> discoveryConfigurer,
			UnaryOperator<LauncherExecutionRequestBuilder> executionConfigurer) {
		var executionListener = mock(TestExecutionListener.class);

		AtomicReference<Instant> startTime = new AtomicReference<>();
		doAnswer(invocation -> {
			startTime.set(Instant.now());
			return null;
		}).when(executionListener).executionStarted(any());

		AtomicReference<Instant> finishTime = new AtomicReference<>();
		doAnswer(invocation -> {
			finishTime.set(Instant.now());
			return null;
		}).when(executionListener).executionFinished(any(), any());

		var launcher = createLauncher(engine);

		var discoveryRequestBuilder = request() //
				.enableImplicitConfigurationParameters(false) //
				.configurationParameter(DISCOVERY_ISSUE_FAILURE_PHASE_PROPERTY_NAME, "execution") //
				.configurationParameter(DEFAULT_DISCOVERY_LISTENER_CONFIGURATION_PROPERTY_NAME, "logging");
		var discoveryRequest = discoveryConfigurer.apply(discoveryRequestBuilder).build();
		var testPlan = launcher.discover(discoveryRequest);

		var executionRequestBuilder = LauncherExecutionRequestBuilder.request(testPlan) //
				.listeners(executionListener);
		var executionRequest = executionConfigurer.apply(executionRequestBuilder).build();
		launcher.execute(executionRequest);

		var inOrder = inOrder(executionListener);
		var testIdentifier = ArgumentCaptor.forClass(TestIdentifier.class);
		var testExecutionResult = ArgumentCaptor.forClass(TestExecutionResult.class);
		inOrder.verify(executionListener).testPlanExecutionStarted(any());
		inOrder.verify(executionListener).executionStarted(testIdentifier.capture());
		inOrder.verify(executionListener).executionFinished(any(), testExecutionResult.capture());
		inOrder.verify(executionListener).testPlanExecutionFinished(any());
		inOrder.verifyNoMoreInteractions();

		return new ReportedData(testPlan, testIdentifier.getValue(), testExecutionResult.getValue(),
			requireNonNull(startTime.get()), requireNonNull(finishTime.get()));
	}

	private static LogRecord findFirstDiscoveryIssueLogRecord(LogRecordListener listener, Level level) {
		return listener.stream(DiscoveryIssueNotifier.class, level) //
				.findFirst() //
				.orElseThrow();
	}

	private record ReportedData(TestPlan testPlan, TestIdentifier testIdentifier,
			TestExecutionResult testExecutionResult, Instant startTime, Instant finishTime) {
	}

}
