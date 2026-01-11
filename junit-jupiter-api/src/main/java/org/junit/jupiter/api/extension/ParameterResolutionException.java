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

	public ParameterResolutionException(String message) {
		super(message);
	}

	public ParameterResolutionException(String message, Throwable cause) {
		super(message, cause);
	}

}
