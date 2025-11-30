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
 * Marks tests that read the default locale but don't use the locale extension themselves.
 *
 * <p>During
 * <a href="https://docs.junit.org/current/user-guide/#writing-tests-parallel-execution" target="_top">parallel test execution</a>,
 * all tests annotated with {@link DefaultLocale}, {@link ReadsDefaultLocale}, and {@link WritesDefaultLocale}
 * are scheduled in a way that guarantees correctness under mutation of shared global state.</p>
 *
 * <p>For more details and examples, see
 * <a href="https://docs.junit.org/current/user-guide/#writing-tests-built-in-extensions-DefaultLocaleAndTimeZone" target="_top">the documentation on <code>@DefaultLocale</code> and <code>@DefaultTimeZone</code></a>.</p>
 *
 * @since 6.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PACKAGE, ElementType.TYPE })
@Inherited
@ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
@API(status = API.Status.STABLE, since = "6.1")
public @interface ReadsDefaultLocale {
}
