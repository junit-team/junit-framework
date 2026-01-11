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

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Interceptor for test discovery and execution by a {@link Launcher} in the
 * context of a {@link LauncherSession}.
 *
 * <p>Interceptors are instantiated once per {@link LauncherSession} and closed
 * after the session is closed. They can
 * {@linkplain #intercept(Invocation) intercept} the following invocations:
 * <ul>
 *     <li>
 *         creation of {@link LauncherSessionListener} instances registered via the
 *         {@link java.util.ServiceLoader ServiceLoader} mechanism
 *     </li>
 *     <li>
 *         creation of {@link Launcher} instances
 *     </li>
 *     <li>
 *         calls to {@link Launcher#discover(LauncherDiscoveryRequest)},
 *         {@link Launcher#execute(TestPlan, TestExecutionListener...)},
 *         {@link Launcher#execute(LauncherDiscoveryRequest, TestExecutionListener...)},
 *         and {@link Launcher#execute(LauncherExecutionRequest)},
 *     </li>
 * </ul>
 *
 * <p>Implementations of this interface can be registered via the
 * {@link java.util.ServiceLoader ServiceLoader} mechanism by additionally
 * setting the {@value LauncherConstants#ENABLE_LAUNCHER_INTERCEPTORS}
 * configuration parameter to {@code true}.
 *
 * <p>A typical use case is to create a custom {@link ClassLoader} in the
 * constructor of the implementing class, replace the
 * {@link Thread#setContextClassLoader(ClassLoader) contextClassLoader} of the
 * current thread while {@link #intercept(Invocation) intercepting} invocations,
 * and close the custom {@code ClassLoader} in {@link #close()}
 *
 * @since 1.10
 * @see Launcher
 * @see LauncherSession
 * @see LauncherConstants#ENABLE_LAUNCHER_INTERCEPTORS
 */
@API(status = MAINTAINED, since = "1.13.3")
public interface LauncherInterceptor {

	/**
	 * Intercept the supplied invocation.
	 *
	 * <p>Implementations must call {@link Invocation#proceed()} exactly once.
	 *
	 * @param invocation the intercepted invocation; never {@code null}
	 * @return the result of the invocation
	 */
	<T extends @Nullable Object> T intercept(Invocation<T> invocation);

	/**
	 * Closes this interceptor.
	 *
	 * <p>Any resources held by this interceptor should be released by this
	 * method.
	 */
	void close();

	/**
	 * An invocation that can be intercepted.
	 *
	 * <p>This interface is not intended to be implemented by clients.
	 */
	interface Invocation<T extends @Nullable Object> {
		T proceed();
	}

}
