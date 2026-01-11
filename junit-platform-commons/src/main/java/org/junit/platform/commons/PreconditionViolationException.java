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

import static org.apiguardian.api.API.Status.STABLE;

import java.io.Serial;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * Thrown if a <em>precondition</em> is violated.
 *
 * @since 1.5
 */
@API(status = STABLE, since = "1.5")
public class PreconditionViolationException extends JUnitException {

	@Serial
	private static final long serialVersionUID = 1L;

	public PreconditionViolationException(String message) {
		super(message);
	}

	public PreconditionViolationException(String message, @Nullable Throwable cause) {
		super(message, cause);
	}

}
