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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.support.FieldContext;
import org.junit.platform.commons.JUnitException;

/**
 * {@code ArgumentsAggregator} is an abstraction for the aggregation of arguments
 * provided by an {@link org.junit.jupiter.params.provider.ArgumentsProvider
 * ArgumentsProvider} for a single invocation of a
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest} method
 * into a single object.
 *
 * <p>An {@code ArgumentsAggregator} is applied to a method parameter of a
 * {@code @ParameterizedTest} method via the {@link AggregateWith @AggregateWith}
 * annotation.
 *
 * <p>The result of the aggregation will be passed as an argument to the
 * {@code @ParameterizedTest} method for the annotated parameter.
 *
 * <p>A common use case is the aggregation of multiple columns from a single line
 * in a CSV file into a domain object such as a {@code Person}, {@code Address},
 * {@code Order}, etc.
 *
 * <p>Implementations must provide a no-args constructor or a single unambiguous
 * constructor to use {@linkplain ParameterResolver parameter resolution}. They
 * should not make any assumptions regarding when they are instantiated or how
 * often they are called. Since instances may potentially be cached and called
 * from different threads, they should be thread-safe.
 *
 * @since 5.2
 * @see AggregateWith
 * @see ArgumentsAccessor
 * @see SimpleArgumentsAggregator
 * @see org.junit.jupiter.params.ParameterizedTest
 */
@API(status = STABLE, since = "5.7")
public interface ArgumentsAggregator {

	/**
	 * Aggregate the arguments contained in the supplied {@code accessor} into a
	 * single object.
	 *
	 * @param accessor an {@link ArgumentsAccessor} containing the arguments to be
	 * aggregated; never {@code null}
	 * @param context the parameter context where the aggregated result is to be
	 * supplied; never {@code null}
	 * @return the aggregated result; may be {@code null} but only if the target
	 * type is a reference type
	 * @throws ArgumentsAggregationException if an error occurs during the
	 * aggregation
	 */
	@Nullable
	Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context)
			throws ArgumentsAggregationException;

	/**
	 * Aggregate the arguments contained in the supplied {@code accessor} into a
	 * single object.
	 *
	 * @param accessor an {@link ArgumentsAccessor} containing the arguments to be
	 * aggregated; never {@code null}
	 * @param context the field context where the aggregated result is to be
	 * injected; never {@code null}
	 * @return the aggregated result; may be {@code null} but only if the target
	 * type is a reference type
	 * @throws ArgumentsAggregationException if an error occurs during the
	 * aggregation
	 * @since 5.13
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	default @Nullable Object aggregateArguments(ArgumentsAccessor accessor, FieldContext context)
			throws ArgumentsAggregationException {
		throw new JUnitException("""
				ArgumentsAggregator does not override the convert(ArgumentsAccessor, FieldContext) method. \
				Please report this issue to the maintainers of %s.""".formatted(getClass().getName()));
	}

}
