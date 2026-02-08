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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @RestoreSystemProperties} is an annotation that is used to restore the
 * entire set of JVM system properties to its original state, or the state of the
 * higher-level container, after execution of the annotated element has completed.
 *
 * <p>Use this annotation when there is a need to programmatically modify system
 * properties in a test method or in {@code @BeforeAll} / {@code @BeforeEach}
 * lifecycle methods. To set or clear a system property, consider
 * {@link SetSystemProperty @SetSystemProperty} or
 * {@link ClearSystemProperty @ClearSystemProperty} instead.
 *
 * <p>{@code @RestoreSystemProperties} can be used on the method and on the class
 * level.
 *
 * <p>When declared on a test method, a snapshot of all JVM system properties is
 * stored prior to that test. The snapshot is created before any {@code @BeforeEach}
 * lifecycle methods in scope and before any {@link SetSystemProperty @SetSystemProperty}
 * or {@link ClearSystemProperty @ClearSystemProperty} annotations on that method.
 * After the test, system properties are restored from the snapshot after all
 * {@code @AfterEach} lifecycle methods have completed.
 *
 * <p>When placed on a test class, a snapshot of all JVM system properties is stored
 * prior to any {@code @BeforeAll} lifecycle methods in scope and before any
 * {@link SetSystemProperty @SetSystemProperty} or
 * {@link ClearSystemProperty @ClearSystemProperty} annotations on that class.
 * After the test class completes, system properties are restored from the snapshot
 * after any {@code @AfterAll} lifecycle methods have completed. In addition, a
 * class-level annotation is inherited by each test method just as if each one were
 * annotated with {@code RestoreSystemProperties}.
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
 * <p><em>Note:</em> The snapshot of the properties object is created using
 * {@link Properties#clone()}. However, this cloned properties object will not
 * include any default values from the original properties object. Consequently,
 * this extension will make a best effort attempt to detect default values and
 * fail if any are detected. For classes that extend {@code Properties}, it is
 * assumed that {@code clone()} is implemented with sufficient fidelity.
 *
 * @since 6.1
 * @see ClearSystemProperty @ClearSystemProperty
 * @see SetSystemProperty @SetSystemProperty
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
@WritesSystemProperty
@ExtendWith(SystemPropertiesExtension.class)
@API(status = EXPERIMENTAL, since = "6.1")
@SuppressWarnings("exports")
public @interface RestoreSystemProperties {
}
