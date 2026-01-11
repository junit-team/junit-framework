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
 * {@code @SelectClasses} specifies the classes to <em>select</em> when running
 * a test suite on the JUnit Platform.
 *
 * @since 1.0
 * @see Suite
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectClass(Class)
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectClass(String)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED, since = "1.0")
public @interface SelectClasses {

	/**
	 * One or more classes to select.
	 *
	 * <p>May be use in conjunction with or instead of {@link #names() names}.
	 */
	Class<?>[] value() default {};

	/**
	 * One or more classes to select by their fully qualified names.
	 *
	 * <p>May be use in conjunction with or instead of {@link #value() value}.
	 *
	 * <p>This attribute is intended to be used when a class cannot be referenced
	 * directly from where this annotation is used &mdash; for example, when a
	 * class is not visible due to being private or package-private.
	 *
	 * @since 1.10
	 */
	@API(status = MAINTAINED, since = "1.13.3")
	String[] names() default {};

}
