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
	 * The type of the data type of the parameter.
	 *
	 * @return the signature of the data type of the parameter.
	 */
	Class<?>[] type() default Void.class;

	/**
	 * The default value used if the parameter is not specified. It can be a
	 * list of values if the parameter supports comma separated values.
	 *
	 * @return the default values.
	 */
	Value defaultValue() default @Value;

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

	@interface Value {

		/**
		 * The {@code short} values to use as the defaults; must not be empty.
		 */
		short[] shorts() default {};

		/**
		 * The {@code byte} values to use as the defaults; must not be empty.
		 */
		byte[] bytes() default {};

		/**
		 * The {@code int} values to use as the defaults; must not be empty.
		 */
		int[] ints() default {};

		/**
		 * The {@code long} values to use as the defaults; must not be empty.
		 */
		long[] longs() default {};

		/**
		 * The {@code float} values to use as the defaults; must not be empty.
		 */
		float[] floats() default {};

		/**
		 * The {@code double} values to use as the defaults; must not be empty.
		 */
		double[] doubles() default {};

		/**
		 * The {@code char} values to use as the defaults; must not be empty.
		 */
		char[] chars() default {};

		/**
		 * The {@code boolean} values to use as the defaults; must not be empty.
		 */
		boolean[] booleans() default {};

		/**
		 * The {@link String} values to use as the defaults; must not be empty.
		 */
		String[] strings() default {};

		/**
		 * The {@link Class} values to use as the defaults; must not be empty.
		 */
		Class<?>[] classes() default {};
	}

}
