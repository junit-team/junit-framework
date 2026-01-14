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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.engine.support.store.NamespacedHierarchicalStore.CloseAction.closeAutoCloseables;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.PreconditionViolationException;
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("launcherSuppliers")
	void launcherRejectsNullRequests(String displayName, Supplier<Launcher> launcherSupplier) {
		assertRejectsNullRequests(launcherSupplier.get());
	}

	private static Stream<Arguments> launcherSuppliers() {
		return Stream.of(
			Arguments.of("session-per-request launcher",
				(Supplier<Launcher>) () -> LauncherFactoryForTestingPurposesOnly.createLauncher(new TestEngineStub())),
			Arguments.of("default launcher",
				(Supplier<Launcher>) () -> new DefaultLauncher(List.of(new TestEngineStub()), List.of(),
					new NamespacedHierarchicalStore<Namespace>(null, closeAutoCloseables()))),
			Arguments.of("delegating launcher", (Supplier<Launcher>) () -> new DelegatingLauncher(new NoOpLauncher())),
			Arguments.of("intercepting launcher",
				(Supplier<Launcher>) () -> new InterceptingLauncher(new NoOpLauncher(),
					new NoOpLauncherInterceptor())));
	}

	private static void assertRejectsNullRequests(Launcher launcher) {
		assertPreconditionViolationExactly(() -> launcher.discover(nullValue(LauncherDiscoveryRequest.class)),
			"LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationExactly(() -> launcher.execute(nullValue(LauncherDiscoveryRequest.class)),
			"LauncherDiscoveryRequest must not be null");
		assertPreconditionViolationExactly(() -> launcher.execute(nullValue(TestPlan.class)),
			"TestPlan must not be null");
		assertPreconditionViolationExactly(() -> launcher.execute(nullValue(LauncherExecutionRequest.class)),
			"LauncherExecutionRequest must not be null");
	}

	private static void assertPreconditionViolationExactly(Runnable action, String expectedMessage) {
		var ex = assertThrows(PreconditionViolationException.class, action::run);
		assertEquals(PreconditionViolationException.class, ex.getClass());
		assertEquals(expectedMessage, ex.getMessage());
		assertNull(ex.getCause());
	}

	/**
	 * Produces a typed {@code null} to avoid overload ambiguity and centralize nullability suppressions.
	 */
	@SuppressWarnings({ "DataFlowIssue", "NullAway", "unused" })
	private static <T> T nullValue(Class<T> type) {
		return null;
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
