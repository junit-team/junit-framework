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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TemporaryClasspathExecutor.withAdditionalClasspathRoot;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;
import static org.junit.platform.launcher.LauncherConstants.DEACTIVATE_LISTENERS_PATTERN_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.ENABLE_LAUNCHER_INTERCEPTORS;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.StoreScope;
import org.junit.jupiter.api.fixtures.TrackLogRecords;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.fakes.TestEngineSpy;
import org.junit.platform.launcher.InterceptedTestEngine;
import org.junit.platform.launcher.InterceptorInjectedLauncherSessionListener;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestLauncherDiscoveryListener;
import org.junit.platform.launcher.TestLauncherInterceptor1;
import org.junit.platform.launcher.TestLauncherInterceptor2;
import org.junit.platform.launcher.TestLauncherSessionListener;
import org.junit.platform.launcher.listeners.AnotherUnusedTestExecutionListener;
import org.junit.platform.launcher.listeners.NoopTestExecutionListener;
import org.junit.platform.launcher.listeners.UnusedTestExecutionListener;

/**
 * @since 1.0
 */
class LauncherFactoryTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void preconditions() {
		assertPreconditionViolationFor(() -> LauncherFactory.create(null));
	}

	@Test
	void testExecutionListenerIsLoadedViaServiceApi() {
		withTestServices(() -> {
			var config = LauncherConfig.builder() //
					.addTestEngines(new TestEngineSpy()) //
					.enableTestEngineAutoRegistration(false) //
					.build();
			var launcher = LauncherFactory.create(config);

			NoopTestExecutionListener.called = false;

			launcher.execute(request().forExecution().build());

			assertTrue(NoopTestExecutionListener.called);
		});
	}

	@Test
	void testExecutionListenersExcludedViaConfigParametersIsNotLoadedViaServiceApi(
			@TrackLogRecords LogRecordListener listener) {
		withTestServices(() -> {
			var value = "org.junit.*.launcher.listeners.Unused*,org.junit.*.launcher.listeners.AnotherUnused*";
			withSystemProperty(DEACTIVATE_LISTENERS_PATTERN_PROPERTY_NAME, value, () -> {
				var config = LauncherConfig.builder() //
						.addTestEngines(new TestEngineSpy()) //
						.enableTestEngineAutoRegistration(false) //
						.build();
				var launcher = LauncherFactory.create(config);

				UnusedTestExecutionListener.called = false;
				AnotherUnusedTestExecutionListener.called = false;

				launcher.execute(request().forExecution().build());

				var logMessage = listener.stream(ServiceLoaderRegistry.class) //
						.map(LogRecord::getMessage) //
						.filter(it -> it.startsWith("Loaded TestExecutionListener instances")) //
						.findAny();
				assertThat(logMessage).isPresent();
				assertThat(logMessage.get()) //
						.contains("NoopTestExecutionListener@") //
						.endsWith(" (excluded classes: [" + UnusedTestExecutionListener.class.getName() + ", "
								+ AnotherUnusedTestExecutionListener.class.getName() + "])");

				assertFalse(UnusedTestExecutionListener.called);
				assertFalse(AnotherUnusedTestExecutionListener.called);
			});
		});
	}

	@Test
	void create() {
		var discoveryRequest = createLauncherDiscoveryRequestForBothStandardEngineExampleClasses();

		var testPlan = LauncherFactory.create().discover(discoveryRequest);
		var roots = testPlan.getRoots();
		assertThat(roots).hasSize(3);

		// @formatter:off
		var ids = roots.stream()
				.map(TestIdentifier::getUniqueId)
				.toList();
		// @formatter:on

		assertThat(ids).containsOnly("[engine:junit-vintage]", "[engine:junit-jupiter]",
			"[engine:junit-platform-suite]");
	}

	@Test
	void createWithConfig() {
		var discoveryRequest = createLauncherDiscoveryRequestForBothStandardEngineExampleClasses();

		var config = LauncherConfig.builder()//
				.enableTestEngineAutoRegistration(false)//
				.addTestEngines(new JupiterTestEngine())//
				.build();

		var testPlan = LauncherFactory.create(config).discover(discoveryRequest);
		var roots = testPlan.getRoots();
		assertThat(roots).hasSize(1);

		// @formatter:off
		var ids = roots.stream()
				.map(TestIdentifier::getUniqueId)
				.toList();
		// @formatter:on

		assertThat(ids).containsOnly("[engine:junit-jupiter]");
	}

	@Test
	void createWithPostDiscoveryFilters() {
		var discoveryRequest = createLauncherDiscoveryRequestForBothStandardEngineExampleClasses();

		var config = LauncherConfig.builder()//
				.addPostDiscoveryFilters(TagFilter.includeTags("test-post-discovery")).build();

		var testPlan = LauncherFactory.create(config).discover(discoveryRequest);
		final var vintage = testPlan.getChildren(UniqueId.parse("[engine:junit-vintage]"));
		assertThat(vintage).isEmpty();

		final var jupiter = testPlan.getChildren(UniqueId.parse("[engine:junit-jupiter]"));
		assertThat(jupiter).hasSize(1);
	}

	@Test
	void applyPostDiscoveryFiltersViaServiceApi() {
		withTestServices(() -> {
			var discoveryRequest = createLauncherDiscoveryRequestForBothStandardEngineExampleClasses();

			var config = LauncherConfig.builder()//
					.build();

			var testPlan = LauncherFactory.create(config).discover(discoveryRequest);
			final var vintage = testPlan.getChildren(UniqueId.parse("[engine:junit-vintage]"));
			assertThat(vintage).isEmpty();

			final var jupiter = testPlan.getChildren(UniqueId.parse("[engine:junit-jupiter]"));
			assertThat(jupiter).hasSize(1);
		});
	}

	@Test
	void notApplyIfDisabledPostDiscoveryFiltersViaServiceApi() {
		withTestServices(() -> {
			var discoveryRequest = createLauncherDiscoveryRequestForBothStandardEngineExampleClasses();

			var config = LauncherConfig.builder()//
					.enablePostDiscoveryFilterAutoRegistration(false).build();

			var testPlan = LauncherFactory.create(config).discover(discoveryRequest);
			final var vintage = testPlan.getChildren(UniqueId.parse("[engine:junit-vintage]"));
			assertThat(vintage).hasSize(1);

			final var jupiter = testPlan.getChildren(UniqueId.parse("[engine:junit-jupiter]"));
			assertThat(jupiter).hasSize(1);
		});
	}

	@Test
	void doesNotDiscoverLauncherDiscoverRequestListenerViaServiceApiWhenDisabled() {
		withTestServices(() -> {
			var config = LauncherConfig.builder() //
					.enableLauncherDiscoveryListenerAutoRegistration(false) //
					.build();
			var launcher = LauncherFactory.create(config);
			TestLauncherDiscoveryListener.called = false;

			launcher.discover(request().build());

			assertFalse(TestLauncherDiscoveryListener.called);
		});
	}

	@Test
	void discoversLauncherDiscoverRequestListenerViaServiceApiByDefault() {
		withTestServices(() -> {
			var launcher = LauncherFactory.create();
			TestLauncherDiscoveryListener.called = false;

			launcher.discover(request().build());

			assertTrue(TestLauncherDiscoveryListener.called);
		});
	}

	@Test
	void doesNotDiscoverLauncherSessionListenerViaServiceApiWhenDisabled() {
		withTestServices(() -> {
			try (var session = (DefaultLauncherSession) LauncherFactory.openSession(
				LauncherConfig.builder().enableLauncherSessionListenerAutoRegistration(false).build())) {

				assertThat(session.getListener()).isSameAs(LauncherSessionListener.NOOP);
			}
		});
	}

	@Test
	void discoversLauncherSessionListenerViaServiceApiByDefault() {
		withTestServices(() -> {
			try (var session = (DefaultLauncherSession) LauncherFactory.openSession()) {
				assertThat(session.getListener()).isEqualTo(new TestLauncherSessionListener());
			}
		});
	}

	@Test
	void createsLauncherInterceptorsBeforeDiscoveringTestEngines() {
		withTestServices(() -> withSystemProperty(ENABLE_LAUNCHER_INTERCEPTORS, "true", () -> {
			var config = LauncherConfig.builder() //
					.enableTestEngineAutoRegistration(true) //
					.build();
			var request = request().build();

			var testPlan = LauncherFactory.create(config).discover(request);

			assertThat(testPlan.getRoots()) //
					.map(TestIdentifier::getUniqueIdObject) //
					.map(UniqueId::getLastSegment) //
					.map(UniqueId.Segment::getValue) //
					.describedAs(
						"Intercepted test engine is added by class loader created by TestLauncherInterceptor1").contains(
							InterceptedTestEngine.ID);
		}));
	}

	@Test
	void appliesLauncherInterceptorsToTestDiscovery() {
		InterceptorInjectedLauncherSessionListener.CALLS = 0;
		withTestServices(() -> withSystemProperty(ENABLE_LAUNCHER_INTERCEPTORS, "true", () -> {
			var engine = new TestEngineSpy() {
				@Override
				public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
					throw new RuntimeException("from discovery");
				}
			};
			var config = LauncherConfig.builder() //
					.enableTestEngineAutoRegistration(false) //
					.addTestEngines(engine) //
					.build();
			var launcher = LauncherFactory.create(config);
			var request = request().build();

			var exception = assertThrows(RuntimeException.class, () -> launcher.discover(request));

			assertThat(exception) //
					.hasRootCauseMessage("from discovery") //
					.hasStackTraceContaining(TestLauncherInterceptor1.class.getName() + ".intercept(") //
					.hasStackTraceContaining(TestLauncherInterceptor2.class.getName() + ".intercept(");
			assertThat(InterceptorInjectedLauncherSessionListener.CALLS).isEqualTo(1);
		}));
	}

	@Test
	void appliesLauncherInterceptorsToTestExecution() {
		InterceptorInjectedLauncherSessionListener.CALLS = 0;
		withTestServices(() -> withSystemProperty(ENABLE_LAUNCHER_INTERCEPTORS, "true", () -> {
			var engine = new TestEngineSpy() {
				@Override
				public void execute(ExecutionRequest request) {
					throw new RuntimeException("from execution");
				}
			};
			var config = LauncherConfig.builder() //
					.enableTestEngineAutoRegistration(false) //
					.addTestEngines(engine) //
					.build();

			AtomicReference<TestExecutionResult> result = new AtomicReference<>();
			var listener = new TestExecutionListener() {
				@Override
				public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
					if (testIdentifier.getParentId().isEmpty()) {
						result.set(testExecutionResult);
					}
				}
			};

			var request = request() //
					.configurationParameter(LauncherConstants.STACKTRACE_PRUNING_ENABLED_PROPERTY_NAME, "false") //
					.forExecution() //
					.listeners(listener) //
					.build();

			var launcher = LauncherFactory.create(config);
			launcher.execute(request);

			assertThat(requireNonNull(result.get()).getThrowable().orElseThrow()) //
					.hasRootCauseMessage("from execution") //
					.hasStackTraceContaining(TestLauncherInterceptor1.class.getName() + ".intercept(") //
					.hasStackTraceContaining(TestLauncherInterceptor2.class.getName() + ".intercept(");
			assertThat(InterceptorInjectedLauncherSessionListener.CALLS).isEqualTo(1);
		}));
	}

	@Test
	void extensionCanReadValueFromSessionStoreAndReadByLauncherSessionListenerOnOpened() {
		var config = LauncherConfig.builder() //
				.addLauncherSessionListeners(new LauncherSessionListenerOpenedExample()) //
				.build();

		try (LauncherSession session = LauncherFactory.openSession(config)) {

			AtomicReference<Throwable> errorRef = new AtomicReference<>();
			var listener = new TestExecutionListener() {
				@Override
				public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
					testExecutionResult.getThrowable().ifPresent(errorRef::set);
				}
			};

			var request = request() //
					.selectors(selectClass(SessionTrackingTestCase.class)) //
					.forExecution() //
					.listeners(listener) //
					.build();

			session.getLauncher().execute(request);

			assertThat(errorRef.get()).isNull();
		}
	}

	@Test
	void extensionCanReadValueFromSessionStoreAndReadByLauncherSessionListenerOnClose() {
		var config = LauncherConfig.builder() //
				.addLauncherSessionListeners(new LauncherSessionListenerClosedExample()) //
				.build();

		try (LauncherSession session = LauncherFactory.openSession(config)) {

			AtomicReference<Throwable> errorRef = new AtomicReference<>();
			var listener = new TestExecutionListener() {
				@Override
				public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
					testExecutionResult.getThrowable().ifPresent(errorRef::set);
				}
			};

			var request = request() //
					.selectors(selectClass(SessionStoringTestCase.class)) //
					.forExecution() //
					.listeners(listener) //
					.build();

			session.getLauncher().execute(request);

			assertThat(errorRef.get()).isNull();
		}
	}

	@Test
	void sessionResourceClosedOnSessionClose() {
		CloseTrackingResource.closed = false;
		var config = LauncherConfig.builder() //
				.addLauncherSessionListeners(new AutoCloseCheckListener()) //
				.build();

		try (LauncherSession session = LauncherFactory.openSession(config)) {
			var request = request() //
					.selectors(selectClass(SessionResourceAutoCloseTestCase.class)) //
					.forExecution() //
					.build();

			session.getLauncher().execute(request);
			assertThat(CloseTrackingResource.closed).isFalse();
		}

		assertThat(CloseTrackingResource.closed).isTrue();
	}

	@Test
	void requestResourceClosedOnExecutionClose() {
		CloseTrackingResource.closed = false;
		var config = LauncherConfig.builder().build();

		try (LauncherSession session = LauncherFactory.openSession(config)) {
			var request = request() //
					.selectors(selectClass(RequestResourceAutoCloseTestCase.class)) //
					.forExecution() //
					.build();

			session.getLauncher().execute(request);

			assertThat(CloseTrackingResource.closed).isTrue();
		}
	}

	@SuppressWarnings("SameParameterValue")
	private static void withSystemProperty(String key, String value, Runnable runnable) {
		var oldValue = System.getProperty(key);
		System.setProperty(key, value);
		try {
			runnable.run();
		}
		finally {
			if (oldValue == null) {
				System.clearProperty(key);
			}
			else {
				System.setProperty(key, oldValue);
			}
		}
	}

	private static void withTestServices(Runnable runnable) {
		withAdditionalClasspathRoot("testservices/", runnable);
	}

	private LauncherDiscoveryRequest createLauncherDiscoveryRequestForBothStandardEngineExampleClasses() {
		// @formatter:off
		return request()
				.selectors(selectClasses(JUnit4Example.class, JUnit5Example.class))
				.enableImplicitConfigurationParameters(false)
				.build();
		// @formatter:on
	}

	@SuppressWarnings({ "NewClassNamingConvention", "JUnitMalformedDeclaration" })
	public static class JUnit4Example {

		@org.junit.Test
		public void testJ4() {
		}

	}

	@SuppressWarnings({ "NewClassNamingConvention", "JUnitMalformedDeclaration" })
	static class JUnit5Example {

		@Tag("test-post-discovery")
		@Test
		void testJ5() {
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(SessionTrackingExtension.class)
	static class SessionTrackingTestCase {

		@Test
		void dummyTest() {
			// Just a placeholder to trigger the extension
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(SessionStoringExtension.class)
	static class SessionStoringTestCase {

		@Test
		void dummyTest() {
			// Just a placeholder to trigger the extension
		}
	}

	static class LauncherSessionListenerOpenedExample implements LauncherSessionListener {
		@Override
		public void launcherSessionOpened(LauncherSession session) {
			session.getStore().put(Namespace.GLOBAL, "testKey", "testValue");
		}
	}

	static class LauncherSessionListenerClosedExample implements LauncherSessionListener {
		@Override
		public void launcherSessionClosed(LauncherSession session) {
			Object storedValue = session.getStore().get(Namespace.GLOBAL, "testKey");
			assertThat(storedValue).isEqualTo("testValue");
		}
	}

	static class SessionTrackingExtension implements BeforeAllCallback {
		@Override
		public void beforeAll(ExtensionContext context) {
			var value = context.getStore(ExtensionContext.Namespace.GLOBAL).get("testKey");
			if (!"testValue".equals(value)) {
				throw new IllegalStateException("Expected 'testValue' but got: " + value);
			}

			value = context.getStore(StoreScope.LAUNCHER_SESSION, ExtensionContext.Namespace.GLOBAL).get("testKey");
			if (!"testValue".equals(value)) {
				throw new IllegalStateException("Expected 'testValue' but got: " + value);
			}
		}
	}

	static class SessionStoringExtension implements BeforeAllCallback {
		@Override
		public void beforeAll(ExtensionContext context) {
			context.getStore(StoreScope.LAUNCHER_SESSION, ExtensionContext.Namespace.GLOBAL).put("testKey",
				"testValue");
		}
	}

	private static class CloseTrackingResource implements AutoCloseable {
		private static boolean closed = false;

		@Override
		public void close() {
			closed = true;
		}

		public boolean isClosed() {
			return closed;
		}
	}

	private static class SessionResourceStoreUsingExtension implements BeforeAllCallback {
		@Override
		public void beforeAll(ExtensionContext context) {
			CloseTrackingResource sessionResource = new CloseTrackingResource();
			context.getStore(StoreScope.LAUNCHER_SESSION, ExtensionContext.Namespace.GLOBAL).put("sessionResource",
				sessionResource);
		}
	}

	private static class RequestResourceStoreUsingExtension implements BeforeAllCallback {
		@Override
		public void beforeAll(ExtensionContext context) {
			CloseTrackingResource requestResource = new CloseTrackingResource();
			context.getStore(StoreScope.EXECUTION_REQUEST, ExtensionContext.Namespace.GLOBAL).put("requestResource",
				requestResource);
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(SessionResourceStoreUsingExtension.class)
	static class SessionResourceAutoCloseTestCase {

		@Test
		void dummyTest() {
			// Just a placeholder to trigger the extension
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@ExtendWith(RequestResourceStoreUsingExtension.class)
	static class RequestResourceAutoCloseTestCase {

		@Test
		void dummyTest() {
			// Just a placeholder to trigger the extension
		}
	}

	private static class AutoCloseCheckListener implements LauncherSessionListener {
		@Override
		public void launcherSessionClosed(LauncherSession session) {
			CloseTrackingResource sessionResource = session //
					.getStore() //
					.get(Namespace.GLOBAL, "sessionResource", CloseTrackingResource.class);

			assertThat(sessionResource).isNotNull();
			assertThat(sessionResource.isClosed()).isFalse();
		}
	}
}
