/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.aggregator;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @AggregateWith} is an annotation that allows one to specify an
 * {@link ArgumentsAggregator}.
 *
 * <p>This annotation may be applied to parameters of a
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass}
 * constructor or its
 * {@link org.junit.jupiter.params.Parameter @Parameter}-annotated fields, or to
 * parameters of a
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest} method
 * in order for an aggregated value to be resolved for the annotated parameter
 * when the parameterized class or method is invoked.
 *
 * <p>{@code @AggregateWith} may also be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics
 * of {@code @AggregateWith}.
 *
 * @since 5.2
 * @see ArgumentsAggregator
 * @see org.junit.jupiter.params.ParameterizedTest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.PARAMETER, ElementType.FIELD })
@Documented
@API(status = STABLE, since = "5.7")
public @interface AggregateWith {

	Class<? extends ArgumentsAggregator> value();

}
