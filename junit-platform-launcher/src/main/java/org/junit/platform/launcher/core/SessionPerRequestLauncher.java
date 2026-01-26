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

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherExecutionRequest;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.8
 */
class SessionPerRequestLauncher implements Launcher {

	private final LauncherListenerRegistry listenerRegistry = new LauncherListenerRegistry();
	private final Function<NamespacedHierarchicalStore<Namespace>, Launcher> launcherFactory;
	private final Supplier<LauncherSessionListener> sessionListenerSupplier;
	private final Supplier<List<LauncherInterceptor>> interceptorFactory;

	SessionPerRequestLauncher(Function<NamespacedHierarchicalStore<Namespace>, Launcher> launcherFactory,
			Supplier<LauncherSessionListener> sessionListenerSupplier,
			Supplier<List<LauncherInterceptor>> interceptorFactory) {
		this.launcherFactory = launcherFactory;
		this.sessionListenerSupplier = sessionListenerSupplier;
		this.interceptorFactory = interceptorFactory;
	}

	@Override
	public void registerLauncherDiscoveryListeners(LauncherDiscoveryListener... listeners) {
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listener array must not contain null elements");
		listenerRegistry.launcherDiscoveryListeners.addAll(listeners);
	}

	@Override
	public void registerTestExecutionListeners(TestExecutionListener... listeners) {
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listener array must not contain null elements");
		listenerRegistry.testExecutionListeners.addAll(listeners);
	}

	@Override
	public TestPlan discover(LauncherDiscoveryRequest discoveryRequest) {
		Preconditions.notNull(discoveryRequest, "discoveryRequest must not be null");
		try (LauncherSession session = createSession()) {
			return session.getLauncher().discover(discoveryRequest);
		}
	}

	@Override
	public void execute(LauncherDiscoveryRequest discoveryRequest, TestExecutionListener... listeners) {
		Preconditions.notNull(discoveryRequest, "discoveryRequest must not be null");
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listener array must not contain null elements");
		try (LauncherSession session = createSession()) {
			session.getLauncher().execute(discoveryRequest, listeners);
		}
	}

	@Override
	public void execute(TestPlan testPlan, TestExecutionListener... listeners) {
		Preconditions.notNull(testPlan, "testPlan must not be null");
		Preconditions.notNull(listeners, "listeners must not be null");
		Preconditions.containsNoNullElements(listeners, "listener array must not contain null elements");
		try (LauncherSession session = createSession()) {
			session.getLauncher().execute(testPlan, listeners);
		}
	}

	@Override
	public void execute(LauncherExecutionRequest executionRequest) {
		Preconditions.notNull(executionRequest, "executionRequest must not be null");
		try (LauncherSession session = createSession()) {
			session.getLauncher().execute(executionRequest);
		}
	}

	private LauncherSession createSession() {
		LauncherSession session = new DefaultLauncherSession(interceptorFactory.get(), sessionListenerSupplier,
			this.launcherFactory);
		Launcher launcher = session.getLauncher();
		listenerRegistry.launcherDiscoveryListeners.getListeners().forEach(
			launcher::registerLauncherDiscoveryListeners);
		listenerRegistry.testExecutionListeners.getListeners().forEach(launcher::registerTestExecutionListeners);
		return session;
	}
}
