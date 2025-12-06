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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @EmptySource} is an {@link ArgumentsSource} which provides a single
 * <em>empty</em> argument to the annotated {@code @ParameterizedClass}
 * or {@code @ParameterizedTest}.
 *
 * <h2>Supported Parameter Types</h2>
 *
 * <p>This argument source will only provide an empty argument for the following
 * parameter types.
 *
 * <ul>
 * <li>{@link java.lang.String}</li>
 * <li>{@link java.util.Collection} and concrete subtypes with a public no-arg constructor</li>
 * <li>{@link java.util.List}</li>
 * <li>{@link java.util.Set}</li>
 * <li>{@link java.util.SortedSet}</li>
 * <li>{@link java.util.NavigableSet}</li>
 * <li>{@link java.util.Map} and concrete subtypes with a public no-arg constructor</li>
 * <li>{@link java.util.SortedMap}</li>
 * <li>{@link java.util.NavigableMap}</li>
 * <li>primitive arrays &mdash; for example {@code int[]}, {@code char[][]}, etc.</li>
 * <li>object arrays &mdash; for example {@code String[]}, {@code Integer[][]}, etc.</li>
 * </ul>
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.4
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.ParameterizedClass
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see NullSource
 * @see NullAndEmptySource
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@API(status = STABLE, since = "5.7")
@ArgumentsSource(EmptyArgumentsProvider.class)
@SuppressWarnings("exports")
public @interface EmptySource {
}
