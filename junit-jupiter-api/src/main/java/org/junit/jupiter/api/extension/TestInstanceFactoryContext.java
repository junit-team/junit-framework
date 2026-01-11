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

import java.util.Optional;

import org.apiguardian.api.API;

/**
 * {@code TestInstanceFactoryContext} encapsulates the <em>context</em> in which
 * a {@linkplain #getTestClass test class} is to be instantiated by a
 * {@link TestInstanceFactory}.
 *
 * @since 5.3
 * @see TestInstanceFactory
 */
@API(status = STABLE, since = "5.7")
public interface TestInstanceFactoryContext {

	/**
	 * Get the test class for this context.
	 *
	 * @return the test class to be instantiated; never {@code null}
	 */
	Class<?> getTestClass();

	/**
	 * Get the instance of the outer class, if available.
	 *
	 * <p>The returned {@link Optional} will be <em>empty</em> unless the
	 * current {@linkplain #getTestClass() test class} is a
	 * {@link org.junit.jupiter.api.Nested @Nested} test class.
	 *
	 * @return an {@code Optional} containing the outer test instance; never
	 * {@code null} but potentially empty
	 * @see org.junit.jupiter.api.Nested
	 */
	Optional<Object> getOuterInstance();

}
