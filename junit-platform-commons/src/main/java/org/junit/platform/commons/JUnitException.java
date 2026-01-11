/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.io.Serial;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Base class for all {@link RuntimeException RuntimeExceptions} thrown
 * by JUnit.
 *
 * @since 1.0
 */
@API(status = STABLE, since = "1.5")
public class JUnitException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public JUnitException(@Nullable String message) {
		super(message);
	}

	public JUnitException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}

	/**
	 * @since 1.13
	 */
	@API(status = MAINTAINED, since = "1.13")
	protected JUnitException(@Nullable String message, @Nullable Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
