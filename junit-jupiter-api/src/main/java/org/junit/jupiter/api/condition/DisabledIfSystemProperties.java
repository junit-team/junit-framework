/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @DisabledIfSystemProperties} is a container for one or more
 * {@link DisabledIfSystemProperty @DisabledIfSystemProperty} declarations.
 *
 * <p>Note, however, that use of the {@code @DisabledIfSystemProperties} container
 * is completely optional since {@code @DisabledIfSystemProperty} is a {@linkplain
 * java.lang.annotation.Repeatable repeatable} annotation.
 *
 * <p>This annotation is not {@link java.lang.annotation.Inherited @Inherited}.
 * Consequently, if you wish to apply the same semantics to a subclass, this
 * annotation must be redeclared on the subclass.
 *
 * @since 5.6
 * @see DisabledIfSystemProperty
 * @see java.lang.annotation.Repeatable
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.6")
public @interface DisabledIfSystemProperties {

	/**
	 * An array of one or more {@link DisabledIfSystemProperty @DisabledIfSystemProperty}
	 * declarations.
	 */
	DisabledIfSystemProperty[] value();

}
