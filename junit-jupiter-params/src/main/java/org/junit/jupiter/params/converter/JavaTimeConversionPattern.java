/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @JavaTimeConversionPattern} is an annotation that allows a date/time
 * conversion pattern to be specified on a parameter of a
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass}
 * or
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}.
 *
 * @since 5.0
 * @see ConvertWith
 * @see org.junit.jupiter.params.ParameterizedClass
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see java.time.format.DateTimeFormatterBuilder#appendPattern(String)
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.7")
@ConvertWith(JavaTimeArgumentConverter.class)
@SuppressWarnings("exports")
public @interface JavaTimeConversionPattern {

	/**
	 * The date/time conversion pattern.
	 *
	 * @see java.time.format.DateTimeFormatterBuilder#appendPattern(String)
	 */
	String value();

	/**
	 * Whether {@code null} argument values are allowed.
	 *
	 * <p>Defaults to {@code false}, in which case a {@code null} value will result in
	 * an exception.
	 *
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	boolean nullable() default false;

}
