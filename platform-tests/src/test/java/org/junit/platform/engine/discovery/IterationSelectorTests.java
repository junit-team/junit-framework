/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectIteration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Array;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.SimpleArgumentsAggregator;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

class IterationSelectorTests {

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
				1         | 1
				1,2       | 1 | 2
				1..3      | 1 | 2 | 3
				1,3       | 1 | 3
				1..3,5..7 | 1 | 2 | 3 | 5 | 6 | 7
			""")
	void collapsesRangesWhenConvertingToIdentifier(String expected,
			@AggregateWith(VarargsAggregator.class) int... iterationIndices) {
		var parent = "parent:value";
		var parentSelector = selectorWithIdentifier(parent);
		var selector = selectIteration(parentSelector, iterationIndices);

		var identifier = selector.toIdentifier().orElseThrow();
		assertEquals("iteration:%s[%s]".formatted(parent, expected), identifier.toString());

		DiscoverySelectorIdentifierParser.Context context = mock();
		when(context.parse(parent)).thenAnswer(__ -> Optional.of(parentSelector));
		assertEquals(selector, new IterationSelector.IdentifierParser().parse(identifier, context).orElseThrow());
	}

	private static DiscoverySelector selectorWithIdentifier(String identifier) {
		DiscoverySelector parent = mock();
		when(parent.toIdentifier()) //
				.thenReturn(Optional.of(DiscoverySelectorIdentifier.parse(identifier)));
		return parent;
	}

	private static class VarargsAggregator extends SimpleArgumentsAggregator {
		@Override
		protected Object aggregateArguments(ArgumentsAccessor accessor, Class<?> targetType,
				AnnotatedElementContext context, int parameterIndex) throws ArgumentsAggregationException {

			Preconditions.condition(targetType.isArray(), () -> "must be an array type, but was " + targetType);
			Class<?> componentType = targetType.getComponentType();
			IntStream indices = IntStream.range(parameterIndex, accessor.size());
			if (componentType == int.class) {
				return indices.map(accessor::getInteger).toArray();
			}
			return indices.mapToObj(index -> accessor.get(index, componentType)).toArray(
				size -> (Object[]) Array.newInstance(componentType, size));
		}
	}
}
