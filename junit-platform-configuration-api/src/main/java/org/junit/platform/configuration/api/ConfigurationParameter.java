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
	 * <p>
	 * If the type is left blank, the type of the default value is used.
	 *
	 * @return the signature of the data type of the parameter.
	 */
	Class<?> type() default Void.class;

	/**
	 * The default value used if the parameter is not specified.
	 * At most one value may be set.
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

	/**
	 * The value to use as the default. At most one value may be set.
	 */
	@interface Value {

		/**
		 * The {@code short} value to use as the default.
		 */
		short[] shortValue() default {};

		/**
		 * The {@code byte} value to use as the default.
		 */
		byte[] byteValue() default {};

		/**
		 * The {@code int} value to use as the default.
		 */
		int[] intValue() default {};

		/**
		 * The {@code long} value to use as the default.
		 */
		long[] longValue() default {};

		/**
		 * The {@code float} value to use as the default.
		 */
		float[] floatValue() default {};

		/**
		 * The {@code double} value to use as the default.
		 */
		double[] doubleValue() default {};

		/**
		 * The {@code char} value to use as the default.
		 */
		char[] charValue() default {};

		/**
		 * The {@code boolean} value to use as the default.
		 */
		boolean[] booleanValue() default {};

		/**
		 * The {@link String} value to use as the default.
		 */
		String[] stringValue() default {};

		/**
		 * The {@link Class} value to use as the default.
		 */
		Class<?>[] classValue() default {};
	}

}
