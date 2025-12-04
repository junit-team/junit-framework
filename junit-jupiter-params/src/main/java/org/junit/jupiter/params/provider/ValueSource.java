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
 * {@code @ValueSource} is a {@linkplain Repeatable repeatable}
 * {@link ArgumentsSource} which provides access to an array of literal values.
 *
 * <p>Supported types include {@link #shorts}, {@link #bytes}, {@link #ints},
 * {@link #longs}, {@link #floats}, {@link #doubles}, {@link #chars},
 * {@link #booleans}, {@link #strings}, and {@link #classes}. Note, however,
 * that only one of the supported types may be specified per
 * {@code @ValueSource} declaration.
 *
 * <p>The supplied literal values will be provided as arguments to the
 * annotated {@code @ParameterizedClass} or {@code @ParameterizedTest}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.0
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.ParameterizedClass
 * @see org.junit.jupiter.params.ParameterizedTest
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(ValueSources.class)
@API(status = STABLE, since = "5.7")
@ArgumentsSource(ValueArgumentsProvider.class)
@SuppressWarnings("exports")
public @interface ValueSource {

	/**
	 * The {@code short} values to use as sources of arguments; must not be empty.
	 *
	 * @since 5.1
	 */
	short[] shorts() default {};

	/**
	 * The {@code byte} values to use as sources of arguments; must not be empty.
	 *
	 * @since 5.1
	 */
	byte[] bytes() default {};

	/**
	 * The {@code int} values to use as sources of arguments; must not be empty.
	 */
	int[] ints() default {};

	/**
	 * The {@code long} values to use as sources of arguments; must not be empty.
	 */
	long[] longs() default {};

	/**
	 * The {@code float} values to use as sources of arguments; must not be empty.
	 *
	 * @since 5.1
	 */
	float[] floats() default {};

	/**
	 * The {@code double} values to use as sources of arguments; must not be empty.
	 */
	double[] doubles() default {};

	/**
	 * The {@code char} values to use as sources of arguments; must not be empty.
	 *
	 * @since 5.1
	 */
	char[] chars() default {};

	/**
	 * The {@code boolean} values to use as sources of arguments; must not be empty.
	 *
	 * @since 5.5
	 */
	boolean[] booleans() default {};

	/**
	 * The {@link String} values to use as sources of arguments; must not be empty.
	 */
	String[] strings() default {};

	/**
	 * The {@link Class} values to use as sources of arguments; must not be empty.
	 *
	 * @since 5.1
	 */
	Class<?>[] classes() default {};

}
