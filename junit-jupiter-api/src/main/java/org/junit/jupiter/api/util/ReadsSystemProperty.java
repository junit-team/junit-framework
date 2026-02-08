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

import org.apiguardian.api.API;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * {@code @ReadsSystemProperty} marks tests that read system properties but do
 * not use the JVM system properties extensions themselves.
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
 * @see WritesSystemProperty @WritesSystemProperty
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
@ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
@API(status = EXPERIMENTAL, since = "6.1")
public @interface ReadsSystemProperty {
}
