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

/**
 * Constants for the use with the
 * {@link ParameterizedClass @ParameterizedClass} and
 * {@link ParameterizedTest @ParameterizedTest} annotations.
 *
 * @since 5.13
 */
@API(status = MAINTAINED, since = "5.13")
public class ParameterizedInvocationConstants {

	/**
	 * Placeholder for the {@linkplain org.junit.jupiter.api.TestInfo#getDisplayName
	 * display name} of a {@code @ParameterizedTest} method: <code>{displayName}</code>
	 *
	 * @since 5.3
	 * @see ParameterizedClass#name()
	 * @see ParameterizedTest#name()
	 */
	public static final String DISPLAY_NAME_PLACEHOLDER = "{displayName}";

	/**
	 * Placeholder for the current invocation index of a {@code @ParameterizedTest}
	 * method (1-based): <code>{index}</code>
	 *
	 * @since 5.3
	 * @see ParameterizedClass#name()
	 * @see ParameterizedTest#name()
	 * @see #DEFAULT_DISPLAY_NAME
	 */
	public static final String INDEX_PLACEHOLDER = "{index}";

	/**
	 * Placeholder for the complete, comma-separated arguments list of the
	 * current invocation of a {@code @ParameterizedTest} method:
	 * <code>{arguments}</code>
	 *
	 * @since 5.3
	 * @see ParameterizedClass#name()
	 * @see ParameterizedTest#name()
	 */
	public static final String ARGUMENTS_PLACEHOLDER = "{arguments}";

	/**
	 * Placeholder for the complete, comma-separated named arguments list
	 * of the current invocation of a {@code @ParameterizedTest} method:
	 * <code>{argumentsWithNames}</code>
	 *
	 * <p>Argument names will be retrieved via the {@link java.lang.reflect.Parameter#getName()}
	 * API if the byte code contains parameter names &mdash; for example, if
	 * the code was compiled with the {@code -parameters} command line argument
	 * for {@code javac}.
	 *
	 * @since 5.6
	 * @see ParameterizedClass#name()
	 * @see ParameterizedTest#name()
	 * @see #ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 */
	public static final String ARGUMENTS_WITH_NAMES_PLACEHOLDER = "{argumentsWithNames}";

	/**
	 * Placeholder for the name of the argument set for the current invocation
	 * of a {@code @ParameterizedTest} method: <code>{argumentSetName}</code>.
	 *
	 * <p>This placeholder can be used when the current set of arguments was created via
	 * {@link org.junit.jupiter.params.provider.Arguments#argumentSet(String, Object...)
	 * argumentSet()}.
	 *
	 * @since 5.11
	 * @see ParameterizedClass#name()
	 * @see ParameterizedTest#name()
	 * @see #ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 * @see org.junit.jupiter.params.provider.Arguments#argumentSet(String, Object...)
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static final String ARGUMENT_SET_NAME_PLACEHOLDER = "{argumentSetName}";

	/**
	 * Placeholder for either {@link #ARGUMENT_SET_NAME_PLACEHOLDER} or
	 * {@link #ARGUMENTS_WITH_NAMES_PLACEHOLDER}, depending on whether the
	 * current set of arguments was created via
	 * {@link org.junit.jupiter.params.provider.Arguments#argumentSet(String, Object...)
	 * argumentSet()}: <code>{argumentSetNameOrArgumentsWithNames}</code>.
	 *
	 * @since 5.11
	 * @see ParameterizedClass#name()
	 * @see ParameterizedTest#name()
	 * @see #ARGUMENT_SET_NAME_PLACEHOLDER
	 * @see #ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 * @see #DEFAULT_DISPLAY_NAME
	 * @see org.junit.jupiter.params.provider.Arguments#argumentSet(String, Object...)
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	public static final String ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER = "{argumentSetNameOrArgumentsWithNames}";

	/**
	 * Default display name pattern for the current invocation of a
	 * {@code @ParameterizedTest} method: {@value}
	 *
	 * <p>Note that the default pattern does <em>not</em> include the
	 * {@linkplain #DISPLAY_NAME_PLACEHOLDER display name} of the
	 * {@code @ParameterizedTest} method.
	 *
	 * @since 5.3
	 * @see ParameterizedClass#name()
	 * @see ParameterizedTest#name()
	 * @see #DISPLAY_NAME_PLACEHOLDER
	 * @see #INDEX_PLACEHOLDER
	 * @see #ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 */
	public static final String DEFAULT_DISPLAY_NAME = ParameterizedInvocationNameFormatter.DEFAULT_DISPLAY_NAME_PATTERN;

	private ParameterizedInvocationConstants() {
	}
}
