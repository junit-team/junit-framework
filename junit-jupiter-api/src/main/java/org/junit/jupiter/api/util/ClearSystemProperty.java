/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @ClearSystemProperty} is an annotation that is used to clear the value
 * of a JVM system property for test execution.
 *
 * <p>The key of the system property must be specified via the {@link #key() key}
 * attribute.
 *
 * <p>{@code @ClearSystemProperty} can be used on the method and on the class level.
 * It is {@linkplain Repeatable repeatable} and {@link Inherited @Inherited} from
 * higher-level containers. If a class is annotated, the configured property will
 * be cleared before every test inside that class.  After a test has completed,
 * the original value of the system property will be restored.
 *
 * <p>During <a href="https://docs.junit.org/current/writing-tests/parallel-execution.html">
 * parallel test execution</a>, all tests annotated with
 * {@link SetSystemProperty @SetSystemProperty},
 * {@link ClearSystemProperty @ClearSystemProperty},
 * {@link ReadsSystemProperty @ReadsSystemProperty}, and
 * {@link WritesSystemProperty @WritesSystemProperty} are scheduled in a way that
 * guarantees correctness under mutation of shared global state.
 *
 * <p>For further details and examples, see the documentation on all JVM system
 * property annotations in the
 * <a href="https://docs.junit.org/current/writing-tests/built-in-extensions.html#system-properties">
 * User Guide</a>.
 *
 * @since 6.1
 * @see SetSystemProperty @SetSystemProperty
 * @see RestoreSystemProperties @RestoreSystemProperties
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@Repeatable(ClearSystemProperty.ClearSystemProperties.class)
@WritesSystemProperty
@ExtendWith(SystemPropertiesExtension.class)
@API(status = EXPERIMENTAL, since = "6.1")
@SuppressWarnings("exports")
public @interface ClearSystemProperty {

	/**
	 * The key of the system property to clear.
	 */
	String key();

	/**
	 * {@code @ClearSystemProperties} is a container for one or more
	 * {@code ClearSystemProperty} declarations.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Inherited
	@WritesSystemProperty
	@API(status = EXPERIMENTAL, since = "6.1")
	@interface ClearSystemProperties {

		/**
		 * An array of one or more {@link ClearSystemProperty @ClearSystemProperty}
		 * declarations.
		 */
		ClearSystemProperty[] value();

	}

}
