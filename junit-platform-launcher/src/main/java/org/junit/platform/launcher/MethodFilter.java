/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.reflect.Method;
import java.util.List;

import org.apiguardian.api.API;

/**
 * {@link PostDiscoveryFilter} that is applied to the fully qualified
 * {@link Method} name without parameters.
 *
 * @since 1.12
 * @see #includeMethodNamePatterns(String...)
 * @see #excludeMethodNamePatterns(String...)
 */
@API(status = MAINTAINED, since = "1.13.3")
public interface MethodFilter extends PostDiscoveryFilter {

	/**
	 * Create a new <em>include</em> {@link MethodFilter} based on the
	 * supplied patterns.
	 *
	 * <p>The patterns are combined using OR semantics, i.e. if the fully
	 * qualified name of a method matches against at least one of the patterns,
	 * the method will be included in the result set.
	 *
	 * @param patterns regular expressions to match against fully qualified
	 * method names; never {@code null}, empty, or containing {@code null}
	 * @see Class#getName()
	 * @see Method#getName()
	 * @see #includeMethodNamePatterns(List)
	 * @see #excludeMethodNamePatterns(String...)
	 */
	static MethodFilter includeMethodNamePatterns(String... patterns) {
		return new IncludeMethodFilter(patterns);
	}

	/**
	 * Create a new <em>include</em> {@link MethodFilter} based on the
	 * supplied patterns.
	 *
	 * <p>The patterns are combined using OR semantics, i.e. if the fully
	 * qualified name of a method matches against at least one of the patterns,
	 * the method will be included in the result set.
	 *
	 * @param patterns regular expressions to match against fully qualified
	 * method names; never {@code null}, empty, or containing {@code null}
	 * @see Class#getName()
	 * @see Method#getName()
	 * @see #includeMethodNamePatterns(String...)
	 * @see #excludeMethodNamePatterns(String...)
	 */
	static MethodFilter includeMethodNamePatterns(List<String> patterns) {
		return includeMethodNamePatterns(patterns.toArray(new String[0]));
	}

	/**
	 * Create a new <em>exclude</em> {@link MethodFilter} based on the
	 * supplied patterns.
	 *
	 * <p>The patterns are combined using OR semantics, i.e. if the fully
	 * qualified name of a method matches against at least one of the patterns,
	 * the method will be excluded from the result set.
	 *
	 * @param patterns regular expressions to match against fully qualified
	 * method names; never {@code null}, empty, or containing {@code null}
	 * @see Class#getName()
	 * @see Method#getName()
	 * @see #excludeMethodNamePatterns(List)
	 * @see #includeMethodNamePatterns(String...)
	 */
	static MethodFilter excludeMethodNamePatterns(String... patterns) {
		return new ExcludeMethodFilter(patterns);
	}

	/**
	 * Create a new <em>exclude</em> {@link MethodFilter} based on the
	 * supplied patterns.
	 *
	 * <p>The patterns are combined using OR semantics, i.e. if the fully
	 * qualified name of a method matches against at least one of the patterns,
	 * the method will be excluded from the result set.
	 *
	 * @param patterns regular expressions to match against fully qualified
	 * method names; never {@code null}, empty, or containing {@code null}
	 * @see Class#getName()
	 * @see Method#getName()
	 * @see #excludeMethodNamePatterns(String...)
	 * @see #includeMethodNamePatterns(String...)
	 */
	static MethodFilter excludeMethodNamePatterns(List<String> patterns) {
		return excludeMethodNamePatterns(patterns.toArray(new String[0]));
	}

}
