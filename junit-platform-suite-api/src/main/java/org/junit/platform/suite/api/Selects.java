/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.suite.api;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @Selects} is a container for one or more
 * {@link Select @Select} declarations.
 *
 * <p>Note, however, that use of the {@code @Selects} container is
 * completely optional since {@code @Select} is a
 * {@linkplain java.lang.annotation.Repeatable repeatable} annotation.
 *
 * @since 1.11
 * @see Select
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED, since = "1.13.3")
public @interface Selects {

	/**
	 * An array of one or more {@link Select @Select} declarations.
	 */
	Select[] value();

}
