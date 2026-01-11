/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.0
 */
class ValueArgumentsProvider extends AnnotationBasedArgumentsProvider<ValueSource> {

	@Override
	protected Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context,
			ValueSource valueSource) {
		Object[] arguments = getArgumentsFromSource(valueSource);
		return Arrays.stream(arguments).map(Arguments::of);
	}

	private Object[] getArgumentsFromSource(ValueSource valueSource) {
		// @formatter:off
		List<?> arrays =
			Stream.of(
				valueSource.shorts(),
				valueSource.bytes(),
				valueSource.ints(),
				valueSource.longs(),
				valueSource.floats(),
				valueSource.doubles(),
				valueSource.chars(),
				valueSource.booleans(),
				valueSource.strings(),
				valueSource.classes()
			)
			.filter(array -> Array.getLength(array) > 0)
			.toList();
		// @formatter:on

		Preconditions.condition(arrays.size() == 1, () -> "Exactly one type of input must be provided in the @"
				+ ValueSource.class.getSimpleName() + " annotation, but there were " + arrays.size());

		Object originalArray = arrays.get(0);
		return IntStream.range(0, Array.getLength(originalArray)) //
				.mapToObj(index -> Array.get(originalArray, index)) //
				.toArray();
	}

}
