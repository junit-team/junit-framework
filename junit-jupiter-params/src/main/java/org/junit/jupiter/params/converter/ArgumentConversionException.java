/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static org.apiguardian.api.API.Status.STABLE;

import java.io.Serial;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;

/**
 * {@code ArgumentConversionException} is an exception that can occur when an
 * object is converted to another object by an implementation of an
 * {@link ArgumentConverter}.
 *
 * @since 5.0
 * @see ArgumentConverter
 */
@API(status = STABLE, since = "5.7")
public class ArgumentConversionException extends JUnitException {

	@Serial
	private static final long serialVersionUID = 1L;

	public ArgumentConversionException(@Nullable String message) {
		super(message);
	}

	public ArgumentConversionException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}

}
