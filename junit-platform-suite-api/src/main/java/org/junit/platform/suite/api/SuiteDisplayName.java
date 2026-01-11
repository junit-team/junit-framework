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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @SuiteDisplayName} is used to declare a {@linkplain #value custom
 * display name} for the annotated test class that is executed as a test suite
 * on the JUnit Platform.
 *
 * <p>Display names are typically used for test reporting in IDEs and build
 * tools and may contain spaces, special characters, and even emoji.
 *
 * <h2>Execution</h2>
 * <p>Test suites can be run on the JUnit Platform the
 * {@code junit-platform-suite-engine} module.
 *
 * @since 1.1
 * @see Suite
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
@API(status = MAINTAINED, since = "1.1")
public @interface SuiteDisplayName {

	/**
	 * Custom display name for the annotated class.
	 *
	 * @return a custom display name; never blank or consisting solely of
	 * whitespace
	 */
	String value();

}
