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
 * {@code @MethodSources} is a simple container for one or more
 * {@link MethodSource @MethodSource} annotations.
 *
 * <p>Note, however, that use of the {@code @MethodSources} container is completely
 * optional since {@code @MethodSource} is a {@linkplain java.lang.annotation.Repeatable
 * repeatable} annotation.
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.11
 * @see MethodSource
 * @see java.lang.annotation.Repeatable
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@API(status = STABLE, since = "5.11")
public @interface MethodSources {

	/**
	 * An array of one or more {@link MethodSource @MethodSource}
	 * annotations.
	 */
	MethodSource[] value();

}
