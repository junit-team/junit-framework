/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.MAINTAINED;

import org.apiguardian.api.API;

/**
 * {@code PreInterruptCallback} defines the API for {@link Extension
 * Extensions} that wish to be called prior to invocations of
 * {@link Thread#interrupt()} by the {@link org.junit.jupiter.api.Timeout}
 * extension.
 *
 * <p>JUnit registers a default implementation that dumps the stacks of all
 * {@linkplain Thread threads} to {@code System.out} if the
 * {@value #THREAD_DUMP_ENABLED_PROPERTY_NAME} configuration parameter is set to
 * {@code true}.
 *
 * @since 5.12
 * @see org.junit.jupiter.api.Timeout
 */
@API(status = MAINTAINED, since = "5.13.3")
public interface PreInterruptCallback extends Extension {

	/**
	 * Property name used to enable dumping the stack of all
	 * {@linkplain Thread threads} to {@code System.out} when a timeout has occurred.
	 *
	 * <p>This behavior is disabled by default.
	 *
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	String THREAD_DUMP_ENABLED_PROPERTY_NAME = "junit.jupiter.execution.timeout.threaddump.enabled";

	/**
	 * Callback that is invoked <em>before</em> a {@link Thread} is interrupted with
	 * {@link Thread#interrupt()}.
	 *
	 * <p>Note: There is no guarantee on which {@link Thread} this callback will be
	 * executed.
	 *
	 * @param preInterruptContext the context with the target {@link Thread}, which will get interrupted.
	 * @param extensionContext the extension context for the callback; never {@code null}
	 * @since 5.12
	 * @see PreInterruptContext
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	void beforeThreadInterrupt(PreInterruptContext preInterruptContext, ExtensionContext extensionContext)
			throws Exception;
}
