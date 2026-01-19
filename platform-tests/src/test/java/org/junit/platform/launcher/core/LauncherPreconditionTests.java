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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.junit.platform.engine.support.store.NamespacedHierarchicalStore.CloseAction.closeAutoCloseables;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

class LauncherPreconditionTests {

	private static final String LISTENERS_MUST_NOT_BE_NULL = "listeners must not be null";
	private static final String LISTENERS_MUST_NOT_CONTAIN_NULL_ELEMENTS = "listener array must not contain null elements";

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void discoverRejectsNullDiscoveryRequest(Launcher launcher) {
		assertPreconditionViolationNotNullFor("discoveryRequest",
				() -> launcher.discover((LauncherDiscoveryRequest) null));
	}

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void executeRejectsNullDiscoveryRequest(Launcher launcher) {
		assertPreconditionViolationNotNullFor("discoveryRequest",
				() -> launcher.execute((LauncherDiscoveryRequest) null));
	}

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void executeRejectsNullTestPlan(Launcher launcher) {
		assertPreconditionViolationNotNullFor("testPlan",
				() -> launcher.execute((TestPlan) null));
	}

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void executeRejectsNullExecutionRequest(Launcher launcher) {
		assertPreconditionViolationNotNullFor("executionRequest",
				() -> launcher.execute((LauncherExecutionRequest) null));
	}

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void rejectNullListenersArray(Launcher launcher) {
		var request = mock(LauncherDiscoveryRequest.class);
		var testPlan = mock(TestPlan.class);

		assertPreconditionViolationFor(
				() -> launcher.registerLauncherDiscoveryListeners((LauncherDiscoveryListener[]) null))
				.withMessage(LISTENERS_MUST_NOT_BE_NULL);

		assertPreconditionViolationFor(
				() -> launcher.registerTestExecutionListeners((TestExecutionListener[]) null))
				.withMessage(LISTENERS_MUST_NOT_BE_NULL);

		assertPreconditionViolationFor(
				() -> launcher.execute(request, (TestExecutionListener[]) null))
				.withMessage(LISTENERS_MUST_NOT_BE_NULL);

		assertPreconditionViolationFor(
				() -> launcher.execute(testPlan, (TestExecutionListener[]) null))
				.withMessage(LISTENERS_MUST_NOT_BE_NULL);
	}

	@ParameterizedTest
	@MethodSource("launchers")
	@SuppressWarnings("NullAway")
	void rejectNullListenerElements(Launcher launcher) {
		var discoveryListener = mock(LauncherDiscoveryListener.class);
		var executionListener = mock(TestExecutionListener.class);
		var request = mock(LauncherDiscoveryRequest.class);
		var testPlan = mock(TestPlan.class);

		assertPreconditionViolationFor(
				() -> launcher.registerLauncherDiscoveryListeners(discoveryListener, null))
				.withMessage(LISTENERS_MUST_NOT_CONTAIN_NULL_ELEMENTS);

		assertPreconditionViolationFor(
				() -> launcher.registerTestExecutionListeners(executionListener, null))
				.withMessage(LISTENERS_MUST_NOT_CONTAIN_NULL_ELEMENTS);

		assertPreconditionViolationFor(
				() -> launcher.execute(request, executionListener, null))
				.withMessage(LISTENERS_MUST_NOT_CONTAIN_NULL_ELEMENTS);

		assertPreconditionViolationFor(
				() -> launcher.execute(testPlan, executionListener, null))
				.withMessage(LISTENERS_MUST_NOT_CONTAIN_NULL_ELEMENTS);
	}

	private static Stream<Arguments> launchers() {
		var engine = new TestEngineStub();

		return Stream.of(
			Arguments.of(Named.of("session-per-request launcher", createSessionPerRequestLauncher(engine))),
			Arguments.of(Named.of("default launcher",
				new DefaultLauncher(List.of(engine), List.of(),
					new NamespacedHierarchicalStore<>(null, closeAutoCloseables())))),
			Arguments.of(Named.of("delegating launcher", new DelegatingLauncher(mock(Launcher.class)))),
			Arguments.of(Named.of("intercepting launcher",
				new InterceptingLauncher(mock(Launcher.class), mock(LauncherInterceptor.class)))));
	}

	private static Launcher createSessionPerRequestLauncher(TestEngineStub engine) {
		LauncherConfig config = LauncherConfig.builder()
			.enableTestEngineAutoRegistration(false)
			.enableLauncherDiscoveryListenerAutoRegistration(false)
			.enableTestExecutionListenerAutoRegistration(false)
			.enablePostDiscoveryFilterAutoRegistration(false)
			.enableLauncherSessionListenerAutoRegistration(false)
			.addTestEngines(engine)
			.build();

		Launcher launcher = LauncherFactory.create(config);
		assertTrue(launcher instanceof SessionPerRequestLauncher,
			"Expected launcher to be a SessionPerRequestLauncher");
		return launcher;
	}
}