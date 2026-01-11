/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

/**
 * {@code @Parameter} is used to signal that a field in a
 * {@code @ParameterizedClass} constitutes a <em>parameter</em> and marks it for
 * field injection.
 *
 * <p>{@code @Parameter} may also be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics of
 * {@code @Parameter}.
 *
 * @since 5.13
 * @see ParameterizedClass
 * @see ArgumentsAccessor
 * @see AggregateWith
 * @see org.junit.jupiter.params.converter.ArgumentConverter
 * @see org.junit.jupiter.params.converter.ConvertWith
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD })
@Documented
@API(status = EXPERIMENTAL, since = "6.0")
public @interface Parameter {

	/**
	 * Constant that indicates that the index of the parameter is unset.
	 */
	int UNSET_INDEX = -1;

	/**
	 * {@return the index of the parameter in the list of parameters}
	 *
	 * <p>Must be {@value #UNSET_INDEX} (the default) for <em>aggregators</em>,
	 * that is any field of type {@link ArgumentsAccessor} or any field
	 * annotated with {@link AggregateWith @AggregateWith}.
	 *
	 * <p>May be omitted if there's a single <em>indexed parameter</em>.
	 * Otherwise, must be unique among all <em>indexed parameters</em> of the
	 * parameterized class and its superclasses.
	 */
	int value() default UNSET_INDEX;

}
