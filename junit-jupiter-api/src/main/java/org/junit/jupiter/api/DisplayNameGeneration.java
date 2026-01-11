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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @DisplayNameGeneration} is used to declare a custom display name
 * generator for the annotated test class.
 *
 * <p>This annotation is <em>inherited</em> from superclasses and implemented
 * interfaces. It is also inherited from {@linkplain Class#getEnclosingClass()
 * enclosing classes} for {@link Nested @Nested} test classes.
 *
 * <p>As an alternative to {@code @DisplayNameGeneration}, a global
 * {@link DisplayNameGenerator} can be configured for the entire test suite via
 * the {@value DisplayNameGenerator#DEFAULT_GENERATOR_PROPERTY_NAME} configuration parameter. See
 * the User Guide for details. Note, however, that a {@code @DisplayNameGeneration}
 * declaration always overrides a global {@code DisplayNameGenerator}.
 *
 * @since 5.4
 * @see DisplayName
 * @see DisplayNameGenerator
 * @see IndicativeSentencesGeneration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@API(status = STABLE, since = "5.7")
public @interface DisplayNameGeneration {

	/**
	 * Custom display name generator.
	 *
	 * @return custom display name generator class
	 */
	Class<? extends DisplayNameGenerator> value();

}
