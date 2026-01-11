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
 * {@code @ExcludeClassNamePatterns} specifies regular expressions that are used
 * to match against fully qualified class names when running a test suite on the
 * JUnit Platform.
 *
 * <p>The patterns are combined using OR semantics: if the fully qualified name
 * of a class matches against at least one of the patterns, the class will be
 * excluded from the test plan.
 *
 * @since 1.0
 * @see Suite
 * @see org.junit.platform.engine.discovery.ClassNameFilter#excludeClassNamePatterns
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@API(status = MAINTAINED, since = "1.0")
public @interface ExcludeClassNamePatterns {

	/**
	 * Regular expressions used to match against fully qualified class names.
	 */
	String[] value();

}
