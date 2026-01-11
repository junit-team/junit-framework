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

import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;

/**
 * {@code TestInstances} encapsulates the <em>test instances</em> of a test.
 *
 * <p>While top-level tests only have a single test instance, nested tests
 * have one additional instance for each enclosing test class.
 *
 * @since 5.4
 * @see ExtensionContext#getTestInstances()
 * @see ExtensionContext#getRequiredTestInstances()
 */
@API(status = STABLE, since = "5.7")
public interface TestInstances {

	/**
	 * Get the innermost test instance.
	 *
	 * <p>The innermost instance is the one closest to the test method.
	 *
	 * @return the innermost test instance; never {@code null}
	 */
	Object getInnermostInstance();

	/**
	 * Get the enclosing test instances, excluding the innermost test instance,
	 * ordered from outermost to innermost.
	 *
	 * @return the enclosing test instances; never {@code null} or containing
	 * {@code null}, but potentially empty
	 */
	List<Object> getEnclosingInstances();

	/**
	 * Get all test instances, ordered from outermost to innermost.
	 *
	 * @return all test instances; never {@code null}, containing {@code null},
	 * or empty
	 */
	List<Object> getAllInstances();

	/**
	 * Find the first test instance that is an instance of the supplied required
	 * type, checking from innermost to outermost.
	 *
	 * @param requiredType the type to search for
	 * @return the first test instance of the required type; never {@code null}
	 * but potentially empty
	 */
	<T> Optional<T> findInstance(Class<T> requiredType);

}
