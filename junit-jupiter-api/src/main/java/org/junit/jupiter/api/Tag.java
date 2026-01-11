/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @Tag} is a {@linkplain Repeatable repeatable} annotation that is
 * used to declare a <em>tag</em> for the annotated test class or test method.
 *
 * <p>Tags are used to filter which tests are executed for a given test
 * plan. For example, a development team may tag tests with values such as
 * {@code "fast"}, {@code "slow"}, {@code "ci-server"}, etc. and then supply a
 * list of tags to be included in or excluded from the current test plan,
 * potentially dependent on the current environment.
 *
 * <h2>Syntax Rules for Tags</h2>
 * <ul>
 * <li>A tag must not be blank.</li>
 * <li>A <em>trimmed</em> tag must not contain whitespace.</li>
 * <li>A <em>trimmed</em> tag must not contain ISO control characters.</li>
 * <li>A <em>trimmed</em> tag must not contain any of the following
 * <em>reserved characters</em>.
 * <ul>
 * <li>{@code ,}: <em>comma</em></li>
 * <li>{@code (}: <em>left parenthesis</em></li>
 * <li>{@code )}: <em>right parenthesis</em></li>
 * <li>{@code &}: <em>ampersand</em></li>
 * <li>{@code |}: <em>vertical bar</em></li>
 * <li>{@code !}: <em>exclamation point</em></li>
 * </ul>
 * </li>
 * </ul>
 *
 * @since 5.0
 * @see Tags
 * @see Test
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(Tags.class)
@API(status = STABLE, since = "5.0")
public @interface Tag {

	/**
	 * The <em>tag</em>.
	 *
	 * <p>Note: the tag will first be {@linkplain String#strip() stripped}. If the
	 * supplied tag is syntactically invalid after trimming, the error will be
	 * logged as a warning, and the invalid tag will be effectively ignored. See
	 * {@linkplain Tag Syntax Rules for Tags}.
	 */
	String value();

}
