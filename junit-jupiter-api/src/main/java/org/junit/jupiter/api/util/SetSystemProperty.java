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
 * {@code @SetSystemProperty} is an annotation that is used to set the value of
 * a JVM system property for test execution.
 *
 * <p>The key and value of the system property must be specified via the
 * {@link #key() key} and {@link #value() value} attributes.
 *
 * <p>This annotation can be used both at the type level and the method level.
 * It is {@linkplain Repeatable repeatable} and {@link Inherited @Inherited} from
 * higher-level containers &mdash; for example, a test method inherits a
 * {@code @SetSystemProperty} declaration from the test class in which the test
 * is declared. If this annotation is declared on a test class (or implemented
 * interface), the configured property will be set before every test inside that
 * class. After a test has completed, the original value of the system property
 * (or the value configured via {@code @SetSystemProperty} at a higher level) will
 * be restored. Note that method-level configuration always overrides class-level
 * configuration.
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
 * @see ClearSystemProperty @ClearSystemProperty
 * @see RestoreSystemProperties @RestoreSystemProperties
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@Repeatable(SetSystemProperty.SetSystemProperties.class)
@WritesSystemProperty
@ExtendWith(SystemPropertiesExtension.class)
@API(status = EXPERIMENTAL, since = "6.1")
@SuppressWarnings("exports")
public @interface SetSystemProperty {

	/**
	 * The key of the system property to set.
	 */
	String key();

	/**
	 * The value of the system property to set.
	 */
	String value();

	/**
	 * {@code @SetSystemProperties} is a container for one or more
	 * {@code SetSystemProperty} declarations.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Inherited
	@WritesSystemProperty
	@API(status = EXPERIMENTAL, since = "6.1")
	@interface SetSystemProperties {

		/**
		 * An array of one or more {@link SetSystemProperty @SetSystemProperty}
		 * declarations.
		 */
		SetSystemProperty[] value();

	}

}
