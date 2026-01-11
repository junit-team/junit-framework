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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.io.Serial;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * {@code TemplateInvocationValidationException} is an exception thrown by a
 * {@link TestTemplateInvocationContextProvider} or
 * {@link ClassTemplateInvocationContextProvider} if a validation fails when
 * while providing or closing {@link java.util.stream.Stream} of invocation
 * contexts.
 *
 * @since 5.13
 */
@API(status = EXPERIMENTAL, since = "6.0")
public class TemplateInvocationValidationException extends JUnitException {

	@Serial
	private static final long serialVersionUID = 1L;

	public TemplateInvocationValidationException(String message) {
		super(message);
	}
}
