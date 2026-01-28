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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationContainsNoNullElementsFor;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotNullFor;
import static org.junit.platform.engine.support.store.NamespacedHierarchicalStore.CloseAction.closeAutoCloseables;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

@ParameterizedClass
@MethodSource("launchers")
@SuppressWarnings({ "NullAway", "DataFlowIssue" })
class LauncherPreconditionTests {

	private final Launcher launcher;

	LauncherPreconditionTests(Launcher launcher) {
		this.launcher = launcher;
	}

	@Test
	void discoverRejectsNullDiscoveryRequest() {
		assertPreconditionViolationNotNullFor("discoveryRequest", () -> launcher.discover(null));
	}

	@Test
	void executeRejectsNullDiscoveryRequest() {
		assertPreconditionViolationNotNullFor("discoveryRequest",
			() -> launcher.execute((LauncherDiscoveryRequest) null));
	}

	@Test
	void executeRejectsNullTestPlan() {
		assertPreconditionViolationNotNullFor("testPlan", () -> launcher.execute((TestPlan) null));
	}

	@Test
	void executeRejectsNullExecutionRequest() {
		assertPreconditionViolationNotNullFor("executionRequest", () -> launcher.execute(null));
	}

	@Test
	void rejectNullLauncherDiscoveryListenersArray() {
		assertPreconditionViolationNotNullFor("listeners",
			() -> launcher.registerLauncherDiscoveryListeners((LauncherDiscoveryListener[]) null));
	}

	@Test
	void rejectNullTestExecutionListenersArray() {
		assertPreconditionViolationNotNullFor("listeners",
			() -> launcher.registerTestExecutionListeners((TestExecutionListener[]) null));
	}

	@Test
	void rejectNullListenersArrayWhenExecutingDiscoveryRequest() {
		var request = mock(LauncherDiscoveryRequest.class);
		assertPreconditionViolationNotNullFor("listeners",
			() -> launcher.execute(request, (TestExecutionListener[]) null));
	}

	@Test
	void rejectNullListenersArrayWhenExecutingTestPlan() {
		var testPlan = mock(TestPlan.class);
		assertPreconditionViolationNotNullFor("listeners",
			() -> launcher.execute(testPlan, (TestExecutionListener[]) null));
	}

	@Test
	void rejectNullElementsInLauncherDiscoveryListeners() {
		var listener = mock(LauncherDiscoveryListener.class);
		assertPreconditionViolationContainsNoNullElementsFor("listener array",
			() -> launcher.registerLauncherDiscoveryListeners(listener, null));
	}

	@Test
	void rejectNullElementsInTestExecutionListeners() {
		var listener = mock(TestExecutionListener.class);
		assertPreconditionViolationContainsNoNullElementsFor("listener array",
			() -> launcher.registerTestExecutionListeners(listener, null));
	}

	@Test
	void rejectNullElementsInListenersWhenExecutingDiscoveryRequest() {
		var request = mock(LauncherDiscoveryRequest.class);
		var listener = mock(TestExecutionListener.class);
		assertPreconditionViolationContainsNoNullElementsFor("listener array",
			() -> launcher.execute(request, listener, null));
	}

	@Test
	void rejectNullElementsInListenersWhenExecutingTestPlan() {
		var testPlan = mock(TestPlan.class);
		var listener = mock(TestExecutionListener.class);
		assertPreconditionViolationContainsNoNullElementsFor("listener array",
			() -> launcher.execute(testPlan, listener, null));
	}

	static Stream<Arguments> launchers() {
		return Stream.of( //
			argumentSet("SessionPerRequestLauncher", createSessionPerRequestLauncher()),
			argumentSet("DefaultLauncher", createDefaultLauncher()),
			argumentSet("DelegatingLauncher", createDelegatingLauncher()),
			argumentSet("InterceptingLauncher", createInterceptingLauncher()) //
		);
	}

	private static SessionPerRequestLauncher createSessionPerRequestLauncher() {
		LauncherConfig config = LauncherConfig.builder() //
				.enableTestEngineAutoRegistration(false) //
				.enableLauncherDiscoveryListenerAutoRegistration(false) //
				.enableTestExecutionListenerAutoRegistration(false) //
				.enablePostDiscoveryFilterAutoRegistration(false) //
				.enableLauncherSessionListenerAutoRegistration(false) //
				.addTestEngines(new TestEngineStub()) //
				.build(); //
		Launcher launcher = LauncherFactory.create(config);
		return assertInstanceOf(SessionPerRequestLauncher.class, launcher);
	}

	private static DefaultLauncher createDefaultLauncher() {
		return new DefaultLauncher( //
			List.of(new TestEngineStub()), //
			List.of(), //
			new NamespacedHierarchicalStore<>(null, closeAutoCloseables()) //
		);
	}

	private static DelegatingLauncher createDelegatingLauncher() {
		return new DelegatingLauncher(mock(Launcher.class));
	}

	private static InterceptingLauncher createInterceptingLauncher() {
		return new InterceptingLauncher(mock(Launcher.class), mock(LauncherInterceptor.class));
	}
}
