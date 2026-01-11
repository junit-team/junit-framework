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

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.support.FieldContext;

/**
 * {@code SimpleArgumentsAggregator} is an abstract base class for
 * {@link ArgumentsAggregator} implementations that do not need to distinguish
 * between fields and method/constructor parameters.
 *
 * @since 5.0
 * @see ArgumentsAggregator
 */
@API(status = EXPERIMENTAL, since = "6.0")
public abstract class SimpleArgumentsAggregator implements ArgumentsAggregator {

	public SimpleArgumentsAggregator() {
	}

	@Override
	public @Nullable Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context)
			throws ArgumentsAggregationException {
		return aggregateArguments(accessor, context.getParameter().getType(), context, context.getIndex());
	}

	@Override
	public @Nullable Object aggregateArguments(ArgumentsAccessor accessor, FieldContext context)
			throws ArgumentsAggregationException {
		return aggregateArguments(accessor, context.getField().getType(), context, context.getParameterIndex());
	}

	protected abstract @Nullable Object aggregateArguments(ArgumentsAccessor accessor, Class<?> targetType,
			AnnotatedElementContext context, int parameterIndex) throws ArgumentsAggregationException;
}
