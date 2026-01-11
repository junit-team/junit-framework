/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.parallel;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @Isolated} is used to declare that the annotated test class should be
 * executed in isolation from other test classes.
 *
 * <p>When a test class is run in isolation, no other test class is executed
 * concurrently. This can be used to enable parallel test execution for the
 * entire test suite while running some tests in isolation (e.g. if they modify
 * some global resource).
 *
 * @since 5.7
 * @see ExecutionMode
 * @see ResourceLock
 */
@API(status = STABLE, since = "5.10")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@ResourceLock(Resources.GLOBAL)
public @interface Isolated {

	/**
	 * The reason this test class needs to run in isolation.
	 *
	 * <p>The supplied string is currently not reported in any way but can be
	 * used for documentation purposes.
	 */
	String value() default "";

}
