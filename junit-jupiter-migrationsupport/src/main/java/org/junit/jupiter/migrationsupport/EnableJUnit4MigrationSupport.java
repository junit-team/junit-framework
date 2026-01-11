/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport;

import static org.apiguardian.api.API.Status.DEPRECATED;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.conditions.IgnoreCondition;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.jupiter.migrationsupport.rules.ExpectedExceptionSupport;
import org.junit.jupiter.migrationsupport.rules.ExternalResourceSupport;
import org.junit.jupiter.migrationsupport.rules.VerifierSupport;

/**
 * {@code EnableJUnit4MigrationSupport} is a class-level annotation that
 * enables all JUnit 4 migration support within JUnit Jupiter.
 *
 * <p>Specifically, this annotation registers all extensions supported by
 * {@link EnableRuleMigrationSupport @EnableRuleMigrationSupport} and provides
 * support for JUnit 4's {@link org.junit.Ignore @Ignore} annotation for
 * disabling test classes and test methods.
 *
 * <p>Technically speaking, {@code @EnableJUnit4MigrationSupport} is a composed
 * annotation which registers all of the following migration extensions:
 * {@link VerifierSupport}, {@link ExternalResourceSupport},
 * {@link ExpectedExceptionSupport}, and {@link IgnoreCondition}. Note, however,
 * that you can optionally register one or more of these extensions explicitly
 * without the use of this composed annotation.
 *
 * @since 5.4
 * @see ExternalResourceSupport
 * @see VerifierSupport
 * @see ExpectedExceptionSupport
 * @see IgnoreCondition
 * @see EnableRuleMigrationSupport
 * @deprecated Please migrate to Jupiter extensions and use
 * {@link org.junit.jupiter.api.Disabled @Disabled} instead.
 */
@SuppressWarnings("removal")
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@API(status = DEPRECATED, since = "6.0")
@Deprecated(since = "6.0", forRemoval = true)
@EnableRuleMigrationSupport
@ExtendWith(IgnoreCondition.class)
public @interface EnableJUnit4MigrationSupport {
}
