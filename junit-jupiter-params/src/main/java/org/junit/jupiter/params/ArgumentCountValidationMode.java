/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.apiguardian.api.API.Status.MAINTAINED;

import org.apiguardian.api.API;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Enumeration of argument count validation modes for
 * {@link ParameterizedClass @ParameterizedClass} and
 * {@link ParameterizedTest @ParameterizedTest}.
 *
 * <p>When an {@link ArgumentsSource} provides more arguments than declared by
 * the parameterized class or method, there might be a bug in the class/method
 * or the {@link ArgumentsSource}. By default, the additional arguments are
 * ignored. {@link ArgumentCountValidationMode} allows you to control how
 * additional arguments are handled.
 *
 * @since 5.12
 * @see ParameterizedClass#argumentCountValidation()
 * @see ParameterizedTest#argumentCountValidation()
 */
@API(status = MAINTAINED, since = "5.13.3")
public enum ArgumentCountValidationMode {

	/**
	 * Use the default validation mode.
	 *
	 * <p>The default validation mode may be changed via the
	 * {@value ArgumentCountValidator#ARGUMENT_COUNT_VALIDATION_KEY}
	 * configuration parameter (see the User Guide for details on configuration
	 * parameters).
	 */
	DEFAULT,

	/**
	 * Use the "none" argument count validation mode.
	 *
	 * <p>When there are more arguments provided than declared by the
	 * parameterized class or method, these additional arguments are ignored.
	 */
	NONE,

	/**
	 * Use the strict argument count validation mode.
	 *
	 * <p>When there are more arguments provided than declared by the
	 * parameterized class or method, this raises an error.
	 */
	STRICT

}
