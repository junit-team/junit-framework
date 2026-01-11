/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MethodSourceParameterResolutionDemo {

	// @formatter:off
	// tag::parameter_resolution_MethodSource_example[]
	@RegisterExtension
	static final IntegerResolver integerResolver = new IntegerResolver();

	@ParameterizedTest
	@MethodSource("factoryMethodWithArguments")
	void testWithFactoryMethodWithArguments(String argument) {
		assertTrue(argument.startsWith("2"));
	}

	static Stream<Arguments> factoryMethodWithArguments(int quantity) {
		return Stream.of(
				arguments(quantity + " apples"),
				arguments(quantity + " lemons")
		);
	}

	static class IntegerResolver implements ParameterResolver {

		@Override
		public boolean supportsParameter(ParameterContext parameterContext,
				ExtensionContext extensionContext) {

			return parameterContext.getParameter().getType() == int.class;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext,
				ExtensionContext extensionContext) {

			return 2;
		}

	}
	// end::parameter_resolution_MethodSource_example[]
	// @formatter:on

}
