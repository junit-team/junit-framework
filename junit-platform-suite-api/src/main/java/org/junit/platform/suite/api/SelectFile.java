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

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @SelectFile} is a {@linkplain Repeatable repeatable} annotation that
 * specifies a file to <em>select</em> when running a test suite on the JUnit
 * Platform.
 *
 * @since 1.8
 * @see Suite
 * @see org.junit.platform.engine.discovery.DiscoverySelectors#selectFile(String, org.junit.platform.engine.discovery.FilePosition)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = STABLE, since = "1.10")
@Repeatable(SelectFiles.class)
public @interface SelectFile {

	/**
	 * The file to select.
	 */
	String value();

	/**
	 * The line number within the file; ignored if not greater than zero.
	 */
	int line() default 0;

	/**
	 * The column number within the file; ignored if the line number is ignored
	 * or if not greater than zero.
	 */
	int column() default 0;

}
