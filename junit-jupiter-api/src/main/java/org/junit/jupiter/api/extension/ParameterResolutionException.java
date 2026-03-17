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

import static org.apiguardian.api.API.Status.STABLE;

import java.io.Serial;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;

/**
 * Thrown if an error is encountered in the configuration or execution of a
 * {@link ParameterResolver}.
 *
 * @since 5.0
 * @see ParameterResolver
 */
@API(status = STABLE, since = "5.0")
public class ParameterResolutionException extends JUnitException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a {@code ParameterResolutionException} with the supplied message.
	 *
	 * @param message the message; never {@code null}
	 */
	public ParameterResolutionException(String message) {
		super(Preconditions.notNull(message, "message must not be null"));
	}

	/**
	 * Construct a {@code ParameterResolutionException} with the supplied message
	 * and cause.
	 *
	 * @param message the message; never {@code null}
	 * @param cause the cause; never {@code null}
	 */
	public ParameterResolutionException(String message, Throwable cause) {
		super(Preconditions.notNull(message, "message must not be null"),
			Preconditions.notNull(cause, "cause must not be null"));
	}

	/**
	 * Get the message, never {@code null}.
	 */
	@Override
	public String getMessage() {
		String message = super.getMessage();
		if (message == null) {
			throw new IllegalStateException("message must not be null");
		}
		return message;
	}

}
