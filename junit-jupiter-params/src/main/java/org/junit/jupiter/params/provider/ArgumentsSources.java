/*
 * Copyright 2015-2026 the original author or authors.
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
 * {@code @ArgumentsSources} is a simple container for one or more
 * {@link ArgumentsSource @ArgumentsSource} annotations.
 *
 * <p>Note, however, that use of the {@code @ArgumentsSources} container is completely
 * optional since {@code @ArgumentsSource} is a {@linkplain java.lang.annotation.Repeatable
 * repeatable} annotation.
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.0
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@API(status = STABLE, since = "5.7")
public @interface ArgumentsSources {

	/**
	 * An array of one or more {@link ArgumentsSource @ArgumentsSource}
	 * annotations.
	 */
	ArgumentsSource[] value();

}
