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
 * {@code @ClearSystemProperty} is a JUnit Jupiter extension to clear the value
 * of a system property for a test execution.
 *
 * <p>The key of the system property to be cleared must be specified via {@link #key()}.
 * After the annotated element has been executed, the original value or the value of the
 * higher-level container is restored.</p>
 *
 * <p>{@code @ClearSystemProperty} can be used on the method and on the class level.
 * It is repeatable and inherited from higher-level containers. If a class is
 * annotated, the configured property will be cleared before every test inside that
 * class.</p>
 *
 * <p>During
 * <a href="https://docs.junit.org/current/writing-tests/parallel-execution.html">parallel test execution</a>,
 * all tests annotated with {@link ClearSystemProperty}, {@link SetSystemProperty}, {@link ReadsSystemProperty}, and {@link WritesSystemProperty}
 * are scheduled in a way that guarantees correctness under mutation of shared global state.</p>
 *
 * <p>For more details and examples, see
 * <a href="https://docs.junit.org/current/writing-tests/built-in-extensions.html#SystemProperty">the documentation on <code>@ClearSystemProperty and @SetSystemProperty</code></a>.</p>
 *
 * @since 6.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@Repeatable(ClearSystemProperty.ClearSystemProperties.class)
@WritesSystemProperty
@ExtendWith(SystemPropertyExtension.class)
@API(status = EXPERIMENTAL, since = "6.1")
@SuppressWarnings("exports")
public @interface ClearSystemProperty {

	/**
	 * The key of the system property to be cleared.
	 */
	String key();

	/**
	 * Containing annotation of repeatable {@code @ClearSystemProperty}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Inherited
	@WritesSystemProperty
	@API(status = EXPERIMENTAL, since = "6.1")
	@interface ClearSystemProperties {

		ClearSystemProperty[] value();

	}

}
