/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments.ArgumentSet;

/**
 * Unit tests for {@link Arguments}.
 *
 * @since 5.0
 */
class ArgumentsTests {

	@Test
	void ofSupportsVarargs() {
		var arguments = of(1, "2", 3.0);

		assertArrayEquals(new Object[] { 1, "2", 3.0 }, arguments.get());
	}

	@Test
	void argumentsSupportsVarargs() {
		var arguments = arguments(1, "2", 3.0);

		assertArrayEquals(new Object[] { 1, "2", 3.0 }, arguments.get());
	}

	@Test
	void ofReturnsSameArrayUsedForCreating() {
		Object[] input = { 1, "2", 3.0 };

		var arguments = of(input);

		assertThat(arguments.get()).isSameAs(input);
	}

	@Test
	void argumentsReturnsSameArrayUsedForCreating() {
		Object[] input = { 1, "2", 3.0 };

		var arguments = arguments(input);

		assertThat(arguments.get()).isSameAs(input);
	}

	@Test
	void fromSupportsList() {
		List<Object> input = Arrays.asList(1, "two", null, 3.0);
		Arguments arguments = Arguments.from(input);

		assertArrayEquals(new Object[] { 1, "two", null, 3.0 }, arguments.get());
	}

	@Test
	void fromSupportsListDefensiveCopy() {
		List<Object> input = new ArrayList<>(Arrays.asList(1, "two", null, 3.0));
		Arguments arguments = Arguments.from(input);

		// Modify input
		input.set(1, "changed");
		input.add("new");

		// Assert that arguments are unchanged
		assertArrayEquals(new Object[] { 1, "two", null, 3.0 }, arguments.get());
	}

	@Test
	void argumentsFromSupportsList() {
		List<Object> input = Arrays.asList("a", 2, null);
		Arguments arguments = Arguments.argumentsFrom(input);

		assertArrayEquals(new Object[] { "a", 2, null }, arguments.get());
	}

	@Test
	void argumentSetSupportsList() {
		List<Object> input = Arrays.asList("x", null, 42);
		ArgumentSet argumentSet = Arguments.argumentSetFrom("list-test", input);

		assertArrayEquals(new Object[] { "x", null, 42 }, argumentSet.get());
		assertThat(argumentSet.getName()).isEqualTo("list-test");
	}

	@Test
	void toListReturnsMutableListOfArguments() {
		Arguments arguments = Arguments.of("a", 2, null);

		List<Object> result = arguments.toList();

		assertThat(result).containsExactly("a", 2, null); // preserves content
		result.add("extra"); // confirms mutability
		assertThat(result).contains("extra");
	}

	@Test
	void toListDoesNotAffectInternalArgumentsState() {
		Arguments arguments = Arguments.of("a", 2, null);

		List<Object> result = arguments.toList();
		result.add("extra"); // mutate the returned list

		// Confirm that internal state was not modified
		List<Object> freshCopy = arguments.toList();
		assertThat(freshCopy).containsExactly("a", 2, null);
	}

	@Test
	void toListWorksOnEmptyArguments() {
		Arguments arguments = Arguments.of();

		List<Object> result = arguments.toList();

		assertThat(result).isEmpty();
		result.add("extra");
		assertThat(result).containsExactly("extra");
	}
}
