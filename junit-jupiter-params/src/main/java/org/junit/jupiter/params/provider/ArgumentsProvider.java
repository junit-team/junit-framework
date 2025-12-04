/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.JUnitException;

/**
 * An {@code ArgumentsProvider} is responsible for
 * {@linkplain #provideArguments(ParameterDeclarations, ExtensionContext) providing}
 * a stream of arguments to be passed to a
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass} or
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}.
 *
 * <p>An {@code ArgumentsProvider} can be registered via the
 * {@link ArgumentsSource @ArgumentsSource} annotation.
 *
 * <p>Implementations must provide a no-args constructor or a single unambiguous
 * constructor to use {@linkplain ParameterResolver parameter resolution}.
 *
 * @since 5.0
 * @see org.junit.jupiter.params.ParameterizedClass
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.provider.Arguments
 * @see org.junit.jupiter.params.support.AnnotationConsumer
 */
@API(status = STABLE, since = "5.7")
public interface ArgumentsProvider {

	/**
	 * Provide a {@link Stream} of {@link Arguments} to be passed to a
	 * {@code @ParameterizedTest} method.
	 *
	 * @param context the current extension context; never {@code null}
	 * @return a stream of arguments; never {@code null}
	 * @deprecated Please implement
	 * {@link #provideArguments(ParameterDeclarations, ExtensionContext)} instead.
	 */
	@Deprecated(since = "5.13")
	@API(status = DEPRECATED, since = "5.13")
	default Stream<? extends Arguments> provideArguments(@SuppressWarnings("unused") ExtensionContext context)
			throws Exception {
		throw new UnsupportedOperationException(
			"Please implement provideArguments(ParameterDeclarations, ExtensionContext) instead.");
	}

	/**
	 * Provide a {@link Stream} of {@link Arguments} to be passed to a
	 * {@code @ParameterizedClass} or {@code @ParameterizedTest}.
	 *
	 * @param parameters the parameter declarations for the parameterized
	 * class or test; never {@code null}
	 * @param context the current extension context; never {@code null}
	 * @return a stream of arguments; never {@code null}
	 * @since 5.13
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	default Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context)
			throws Exception {
		try {
			return provideArguments(context);
		}
		catch (Exception e) {
			String message = """
					ArgumentsProvider does not override the provideArguments(ParameterDeclarations, ExtensionContext) method. \
					Please report this issue to the maintainers of %s.""".formatted(
				getClass().getName());
			throw new JUnitException(message, e);
		}
	}

}
