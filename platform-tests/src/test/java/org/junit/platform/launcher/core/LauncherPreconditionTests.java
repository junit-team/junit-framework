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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.support.store.NamespacedHierarchicalStore.CloseAction.closeAutoCloseables;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.TestPlan;

class LauncherPreconditionTests {

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void discoverRejectsNullDiscoveryRequest(Launcher launcher) {
		assertPreconditionViolationExactly(() -> launcher.discover((LauncherDiscoveryRequest) null),
			"DiscoveryRequest must not be null");
	}

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void executeRejectsNullDiscoveryRequest(Launcher launcher) {
		assertPreconditionViolationExactly(() -> launcher.execute((LauncherDiscoveryRequest) null),
			"DiscoveryRequest must not be null");
	}

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void executeRejectsNullTestPlan(Launcher launcher) {
		assertPreconditionViolationExactly(() -> launcher.execute((TestPlan) null), "TestPlan must not be null");
	}

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void executeRejectsNullExecutionRequest(Launcher launcher) {
		assertPreconditionViolationExactly(() -> launcher.execute((LauncherExecutionRequest) null),
			"ExecutionRequest must not be null");
	}

	private static Stream<Arguments> launchers() {
		var engine = new TestEngineStub();

		return Stream.of(
			Arguments.of(Named.of("session-per-request launcher", createSessionPerRequestLauncher(engine))),

			Arguments.of(Named.of("default launcher",
				new DefaultLauncher(List.of(engine), List.of(),
					new NamespacedHierarchicalStore<Namespace>(null, closeAutoCloseables())))),

			Arguments.of(Named.of("delegating launcher", new DelegatingLauncher(mock(Launcher.class)))),

			Arguments.of(Named.of("intercepting launcher",
				new InterceptingLauncher(mock(Launcher.class), mock(LauncherInterceptor.class)))));
	}

	/**
	 * Creating an equivalent LauncherConfig (service loading disabled) and then
	 * assert LauncherFactory creates a SessionPerRequestLauncher.
	 */
	private static Launcher createSessionPerRequestLauncher(TestEngineStub engine) {
		LauncherConfig config = LauncherConfig.builder() //
			.enableTestEngineAutoRegistration(false) //
			.enableLauncherDiscoveryListenerAutoRegistration(false) //
			.enableTestExecutionListenerAutoRegistration(false) //
			.enablePostDiscoveryFilterAutoRegistration(false) //
			.enableLauncherSessionListenerAutoRegistration(false) //
			.addTestEngines(engine) //
			.build();

		Launcher launcher = LauncherFactory.create(config);
		assertTrue(launcher instanceof SessionPerRequestLauncher,
			"Expected Launcher to create a SessionPerRequestLauncher");
		return launcher;
	}

	private static void assertPreconditionViolationExactly(Runnable action, String expectedMessage) {
		var ex = assertThrows(PreconditionViolationException.class, action::run);
		assertEquals(PreconditionViolationException.class, ex.getClass());
		assertEquals(expectedMessage, ex.getMessage());
		assertNull(ex.getCause());
	}
}
