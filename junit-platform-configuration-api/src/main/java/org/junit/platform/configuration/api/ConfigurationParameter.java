/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.configuration.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Marks a field as a configuration parameter for a test engine.
 * <p>
 * This annotation should be used to facilitate the automated
 * generation of documentation.
 */
@API(status = API.Status.EXPERIMENTAL)
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface ConfigurationParameter {

	/**
	 * Specifies that the parameter is deprecated.
	 *
	 * @return details about the deprecation.
	 */
	Deprecation deprecation() default @Deprecation;

	@interface Deprecation {
		/**
		 * A brief description of why the parameter was deprecated.
		 * <p>
		 * The description should be one or more short paragraphs, ending with a period.
		 *
		 * @return a description of why the parameter was deprecated.
		 */
		String reason() default "";

		/**
		 * The full name of the replacement parameter.
		 *
		 * @return the full name of the replacement parameter.
		 */
		String replacement() default "";

		/**
		 * The version of the API when the parameter was deprecated.
		 *
		 * @return the version of the API when the parameter was deprecated.
		 */
		String since() default "";
	}
}
