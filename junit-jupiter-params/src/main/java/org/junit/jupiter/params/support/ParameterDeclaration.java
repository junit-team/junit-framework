/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.support;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * {@code ParameterDeclaration} encapsulates the <em>declaration</em> of an
 * indexed {@code @ParameterizedClass} or {@code @ParameterizedTest} parameter.
 *
 * @since 5.13
 * @see ParameterDeclarations
 */
@API(status = MAINTAINED, since = "6.0.2")
public interface ParameterDeclaration {

	/**
	 * {@return the {@link AnnotatedElement} that declares the parameter; never
	 * {@code null}}
	 *
	 * <p>This is either a {@link java.lang.reflect.Parameter} or a
	 * {@link java.lang.reflect.Field}.
	 */
	AnnotatedElement getAnnotatedElement();

	/**
	 * {@return the type of the parameter; never {@code null}}
	 */
	Class<?> getParameterType();

	/**
	 * {@return the index of the parameter}
	 */
	int getParameterIndex();

	/**
	 * {@return the name of the parameter, if available; never {@code null} but
	 * potentially empty}
	 */
	Optional<String> getParameterName();

}
