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

import static org.apiguardian.api.API.Status.STABLE;

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
 * Marks tests that <em>write</em> the default locale but don't use the
 * {@link DefaultLocale @DefaultLocale} extension themselves.
 *
 * <p>During
 * <a href="https://docs.junit.org/current/writing-tests/parallel-execution.html">parallel test execution</a>,
 * all tests annotated with {@link DefaultLocale @DefaultLocale},
 * {@link ReadsDefaultLocale @ReadsDefaultLocale}, and
 * {@link WritesDefaultLocale} are scheduled in a way that guarantees
 * correctness under mutation of shared global state.
 *
 * <p>For more details and examples, see the
 * <a href="https://docs.junit.org/current/writing-tests/built-in-extensions.html#DefaultLocale">User Guide</a>.
 *
 * @since 6.1
 * @see DefaultLocale
 * @see ReadsDefaultLocale
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
@Inherited
@ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ_WRITE)
@API(status = STABLE, since = "6.1")
public @interface WritesDefaultLocale {
}
