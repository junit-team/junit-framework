/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.api;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @ExcludeTags} specifies the
 * {@linkplain #value tags or tag expressions} to be excluded when running a
 * test suite on the JUnit Platform.
 *
 * <h2>Tag Expressions</h2>
 *
 * <p>Tag expressions are boolean expressions with the following allowed
 * operators: {@code !} (not), {@code &} (and) and {@code |} (or). Parentheses
 * can be used to adjust for operator precedence. Please refer to the
 * <a href="https://docs.junit.org/current/running-tests/tags.html#expressions">JUnit User Guide</a>
 * for usage examples.
 *
 * <h2>Syntax Rules for Tags</h2>
 * <ul>
 * <li>A tag must not be blank.</li>
 * <li>A stripped tag must not contain whitespace.</li>
 * <li>A stripped tag must not contain ISO control characters.</li>
 * <li>A stripped tag must not contain <em>reserved characters</em>.</li>
 * </ul>
 *
 * <p><em>Reserved characters</em> that are not permissible as part of a tag name.
 *
 * <ul>
 * <li>{@code ","}</li>
 * <li>{@code "("}</li>
 * <li>{@code ")"}</li>
 * <li>{@code "&"}</li>
 * <li>{@code "|"}</li>
 * <li>{@code "!"}</li>
 * </ul>
 *
 * @since 1.0
 * @see Suite
 * @see org.junit.platform.launcher.TagFilter#excludeTags
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED, since = "1.0")
public @interface ExcludeTags {

	/**
	 * One or more tags to exclude.
	 *
	 * <p>Note: each tag will be {@linkplain String#strip() stripped} and
	 * validated according to the <em>Syntax Rules for Tags</em> (see
	 * {@linkplain ExcludeTags class-level Javadoc} for details).
	 */
	String[] value();

}
