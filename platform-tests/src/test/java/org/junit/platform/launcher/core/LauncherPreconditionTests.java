/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.engine.support.store.NamespacedHierarchicalStore.CloseAction.closeAutoCloseables;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

@SuppressWarnings("NullAway")
class LauncherPreconditionTests {

	private final Launcher launcher = LauncherFactoryForTestingPurposesOnly.createLauncher(new TestEngineStub());

	@Test
	@SuppressWarnings("DataFlowIssue")
	void sessionPerRequestLauncherRejectsNullDiscoveryRequest() {
		assertPreconditionViolationFor(() -> launcher.discover(null)) //
				.withMessageContaining("LauncherDiscoveryRequest must not be null");
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void sessionPerRequestLauncherRejectsNullExecutionRequests() {
		assertPreconditionViolationFor(() -> launcher.execute((LauncherDiscoveryRequest) null)) //
				.withMessageContaining("LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationFor(() -> launcher.execute((TestPlan) null)) //
				.withMessageContaining("TestPlan must not be null");
		assertPreconditionViolationFor(() -> launcher.execute((LauncherExecutionRequest) null)) //
				.withMessageContaining("LauncherExecutionRequest must not be null");
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void defaultLauncherRejectsNullRequests() {
		var defaultLauncher = new DefaultLauncher(List.of(new TestEngineStub()), List.of(),
				new NamespacedHierarchicalStore<Namespace>(null, closeAutoCloseables()));

		assertPreconditionViolationFor(() -> defaultLauncher.discover(null)) //
				.withMessageContaining("LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationFor(() -> defaultLauncher.execute((LauncherDiscoveryRequest) null)) //
				.withMessageContaining("LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationFor(() -> defaultLauncher.execute((TestPlan) null)) //
				.withMessageContaining("TestPlan must not be null");
		assertPreconditionViolationFor(() -> defaultLauncher.execute((LauncherExecutionRequest) null)) //
				.withMessageContaining("LauncherExecutionRequest must not be null");
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void delegatingLauncherRejectsNullRequests() {
		var delegatingLauncher = new DelegatingLauncher(new NoOpLauncher());

		assertPreconditionViolationFor(() -> delegatingLauncher.discover(null)) //
				.withMessageContaining("LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationFor(() -> delegatingLauncher.execute((LauncherDiscoveryRequest) null)) //
				.withMessageContaining("LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationFor(() -> delegatingLauncher.execute((TestPlan) null)) //
				.withMessageContaining("TestPlan must not be null");
		assertPreconditionViolationFor(() -> delegatingLauncher.execute((LauncherExecutionRequest) null)) //
				.withMessageContaining("LauncherExecutionRequest must not be null");
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void interceptingLauncherRejectsNullRequests() {
		var interceptingLauncher = new InterceptingLauncher(new NoOpLauncher(), new NoOpLauncherInterceptor());

		assertPreconditionViolationFor(() -> interceptingLauncher.discover(null)) //
				.withMessageContaining("LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationFor(() -> interceptingLauncher.execute((LauncherDiscoveryRequest) null)) //
				.withMessageContaining("LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationFor(() -> interceptingLauncher.execute((TestPlan) null)) //
				.withMessageContaining("TestPlan must not be null");
		assertPreconditionViolationFor(() -> interceptingLauncher.execute((LauncherExecutionRequest) null)) //
				.withMessageContaining("LauncherExecutionRequest must not be null");
	}

	private static final class NoOpLauncher implements Launcher {

		@Override
		public void registerLauncherDiscoveryListeners(LauncherDiscoveryListener... listeners) {
		}

		@Override
		public void registerTestExecutionListeners(TestExecutionListener... listeners) {
		}

		@Override
		public TestPlan discover(LauncherDiscoveryRequest launcherDiscoveryRequest) {
			throw new AssertionError("unexpected call");
		}

		@Override
		public void execute(LauncherDiscoveryRequest launcherDiscoveryRequest, TestExecutionListener... listeners) {
			throw new AssertionError("unexpected call");
		}

		@Override
		public void execute(TestPlan testPlan, TestExecutionListener... listeners) {
			throw new AssertionError("unexpected call");
		}

		@Override
		public void execute(LauncherExecutionRequest launcherExecutionRequest) {
			throw new AssertionError("unexpected call");
		}
	}

	private static final class NoOpLauncherInterceptor implements LauncherInterceptor {

		@Override
		public <T> T intercept(Invocation<T> invocation) {
			return invocation.proceed();
		}

		@Override
		public void close() {
		}
	}
}