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
 * {@code PreInterruptContext} encapsulates the <em>context</em> in which an
 * {@link PreInterruptCallback#beforeThreadInterrupt(PreInterruptContext, ExtensionContext) beforeThreadInterrupt} method is called.
 *
 * @since 5.12
 * @see PreInterruptCallback
 */
@API(status = MAINTAINED, since = "5.13.3")
public interface PreInterruptContext {

	/**
	 * Get the {@link Thread} which will be interrupted.
	 *
	 * @return the Thread; never {@code null}
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	Thread getThreadToInterrupt();
}
