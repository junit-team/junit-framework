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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.listeners.discovery.LauncherDiscoveryListeners;
import org.junit.platform.launcher.listeners.session.LauncherSessionListeners;

class ListenerRegistry<T> {

	private final Function<List<T>, T> compositeListenerFactory;

	static ListenerRegistry<LauncherSessionListener> forLauncherSessionListeners() {
		return create(LauncherSessionListeners::composite);
	}

	static ListenerRegistry<LauncherDiscoveryListener> forLauncherDiscoveryListeners() {
		return create(LauncherDiscoveryListeners::composite);
	}

	static ListenerRegistry<TestExecutionListener> forTestExecutionListeners() {
		return create(CompositeTestExecutionListener::new);
	}

	static ListenerRegistry<EngineExecutionListener> forEngineExecutionListeners() {
		return create(CompositeEngineExecutionListener::new);
	}

	static <T> ListenerRegistry<T> create(Function<List<T>, T> compositeListenerFactory) {
		return new ListenerRegistry<>(compositeListenerFactory);
	}

	static <T> ListenerRegistry<T> copyOf(ListenerRegistry<T> source) {
		ListenerRegistry<T> registry = new ListenerRegistry<>(source.compositeListenerFactory);
		if (!source.listeners.isEmpty()) {
			registry.addAll(source.listeners);
		}
		return registry;
	}

	private final ArrayList<T> listeners = new ArrayList<>();

	private ListenerRegistry(Function<List<T>, T> compositeListenerFactory) {
		this.compositeListenerFactory = compositeListenerFactory;
	}

	ListenerRegistry<T> add(T listener) {
		Preconditions.notNull(listener, "listener must not be null");
		this.listeners.add(listener);
		return this;
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	final ListenerRegistry<T> addAll(T... listeners) {
		Preconditions.notEmpty(listeners, "listeners array must not be null or empty");
		return addAll(Arrays.asList(listeners));
	}

	ListenerRegistry<T> addAll(Collection<? extends T> listeners) {
		Preconditions.notEmpty(listeners, "listeners collection must not be null or empty");
		Preconditions.containsNoNullElements(listeners, "individual listeners must not be null");
		this.listeners.addAll(listeners);
		return this;
	}

	T getCompositeListener() {
		this.listeners.trimToSize();
		return compositeListenerFactory.apply(this.listeners);
	}

	List<T> getListeners() {
		return Collections.unmodifiableList(this.listeners);
	}

}
