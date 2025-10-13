/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @ConvertWith} is an annotation that allows one to specify an explicit
 * {@link ArgumentConverter}.
 *
 * <p>This annotation may be applied to parameters of a
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass}
 * constructor or its
 * {@link org.junit.jupiter.params.Parameter @Parameter}-annotated fields, or to
 * parameters of a
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest} method
 * which need to have their {@code Arguments} converted before consuming them.
 *
 * @since 5.0
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see org.junit.jupiter.params.converter.ArgumentConverter
 */
@API(status = STABLE, since = "5.7")
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.PARAMETER, ElementType.FIELD })
public @interface ConvertWith {

	/**
	 * The type of {@link ArgumentConverter} to use.
	 */
	Class<? extends ArgumentConverter> value();

}
