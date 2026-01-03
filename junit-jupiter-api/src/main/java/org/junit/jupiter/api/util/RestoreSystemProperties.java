/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.util;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @RestoreSystemProperties} is a JUnit Jupiter extension to restore the entire set of
 * system properties to the original value, or the value of the higher-level container, after the
 * annotated element is complete.
 *
 * <p>Use this annotation when there is a need programmatically modify system properties in a test
 * method or in {@code @BeforeAll} / {@code @BeforeEach} blocks.
 * To set or clear a system property, consider {@link SetSystemProperty @SetSystemProperty} or
 * {@link ClearSystemProperty @ClearSystemProperty} instead.</p>
 *
 * <p>{@code RestoreSystemProperties} can be used on the method and on the class level.
 * When placed on a test method, a snapshot of system properties is stored prior to that test.
 * The snapshot is created before any {@code @BeforeEach} blocks in scope and before any
 * {@link SetSystemProperty @SetSystemProperty} or {@link ClearSystemProperty @ClearSystemProperty}
 * annotations on that method. After the test, system properties are restored from the
 * snapshot after any {@code @AfterEach} have completed.
 *
 * <p>When placed on a test class, a snapshot of system properties is stored prior to any
 * {@code @BeforeAll} blocks in scope and before any {@link SetSystemProperty @SetSystemProperty}
 * or {@link ClearSystemProperty @ClearSystemProperty} annotations on that class.
 * After the test class completes, system properties are restored from the snapshot after any
 * {@code @AfterAll} blocks have completed.
 * In addition, a class level annotation is inherited by each test method just as if each one was
 * annotated with {@code RestoreSystemProperties}.
 *
 * <p>During
 * <a href="https://docs.junit.org/current/user-guide/#writing-tests-parallel-execution" target="_top">parallel test execution</a>,
 * all tests annotated with {@link RestoreSystemProperties}, {@link SetSystemProperty},
 * {@link ReadsSystemProperty}, and {@link WritesSystemProperty}
 * are scheduled in a way that guarantees correctness under mutation of shared global state.</p>
 *
 * <p>For more details and examples, see
 * <a href="https://docs.junit.org/current/user-guide/#writing-tests-built-in-extensions-SystemProperty" target="_top">the documentation on
 * <code>@ClearSystemProperty</code>, <code>@SetSystemProperty</code>, and <code>@RestoreSystemProperties</code></a>.</p>
 *
 * <p><em>Note:</em> The system properties object normally acts like a map of strings. While strongly
 * discouraged, it is possible to use non-string keys and values. It is also possible to
 * {@linkplain java.util.Properties create nested propeties with inherited default
 * values}. {@code @RestoreSystemProperties} restores the original properties object with all of its
 * potential richness _after_ the annotated scope is complete.
 * However _during_ the execution of the annotated scope, the system properties are set to
 * a cloned properties object where properties with non-string values are removed and nested properties
 * are flattened into a non-nested instance that has the same effective values.
 *
 * @since 6.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@WritesSystemProperty
@ExtendWith(SystemPropertyExtension.class)
@API(status = STABLE, since = "6.1")
@SuppressWarnings("exports")
public @interface RestoreSystemProperties {
}
