/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.aggregator;

import static org.apiguardian.api.API.Status.STABLE;

import java.io.Serial;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * {@code ArgumentAccessException} is an exception thrown by an
 * {@link ArgumentsAccessor} if an error occurs while accessing
 * or converting an argument.
 *
 * @since 5.2
 * @see ArgumentsAccessor
 */
@API(status = STABLE, since = "5.7")
public class ArgumentAccessException extends JUnitException {

	@Serial
	private static final long serialVersionUID = 1L;

	public ArgumentAccessException(String message) {
		super(message);
	}

	public ArgumentAccessException(String message, Throwable cause) {
		super(message, cause);
	}

}
