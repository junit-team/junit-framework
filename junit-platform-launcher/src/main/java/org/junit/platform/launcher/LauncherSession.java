/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.junit.platform.engine.support.store.Namespace;
import org.junit.platform.engine.support.store.NamespacedHierarchicalStore;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * The {@code LauncherSession} API is the main entry point for client code that
 * wishes to repeatedly <em>discover</em> and <em>execute</em> tests using one
 * or more {@linkplain org.junit.platform.engine.TestEngine test engines}.
 *
 * @since 1.8
 * @see Launcher
 * @see LauncherSessionListener
 * @see LauncherFactory
 */
@API(status = STABLE, since = "1.10")
public interface LauncherSession extends AutoCloseable {

	/**
	 * Get the {@link Launcher} associated with this session.
	 *
	 * <p>Any call to the launcher returned by this method after the session has
	 * been closed will throw an exception.
	 */
	Launcher getLauncher();

	/**
	 * Close this session and notify all registered
	 * {@link LauncherSessionListener LauncherSessionListeners}.
	 *
	 * @apiNote The behavior of calling this method concurrently with any call
	 * to the {@link Launcher} returned by {@link #getLauncher()} is currently
	 * undefined.
	 */
	@Override
	void close();

	/**
	 * Get the {@link NamespacedHierarchicalStore} associated with this session.
	 *
	 * <p>All stored values that implement {@link AutoCloseable} are notified by
	 * invoking their {@code close()} methods when this session is closed.
	 *
	 * <p>Any call to the store returned by this method after the session has
	 * been closed will throw an exception.
	 *
	 * @since 1.13
	 * @see NamespacedHierarchicalStore
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	NamespacedHierarchicalStore<Namespace> getStore();

}
