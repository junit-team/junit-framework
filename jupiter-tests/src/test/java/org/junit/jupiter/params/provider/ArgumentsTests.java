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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

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
	void fromSupportsCollection() {
		Collection<@Nullable Object> input = Arrays.asList(1, "two", null, 3.0);
		var arguments = Arguments.from(input);

		assertArrayEquals(new Object[] { 1, "two", null, 3.0 }, arguments.get());
	}

	@Test
	void fromSupportsIterable() {
		var input = new IterableWithNullableElements(1, "two", null, 3.0);
		var arguments = Arguments.from(input);

		assertArrayEquals(new Object[] { 1, "two", null, 3.0 }, arguments.get());
	}

	@Test
	void fromSupportsListDefensiveCopy() {
		var input = new ArrayList<@Nullable Object>(asList(1, "two", null, 3.0));
		var arguments = Arguments.from(input);

		// Modify input
		input.set(1, "changed");
		input.add("new");

		// Assert that arguments are unchanged
		assertArrayEquals(new Object[] { 1, "two", null, 3.0 }, arguments.get());
	}

	@Test
	void argumentsFromSupportsCollection() {
		Collection<@Nullable Object> input = asList("a", 2, null);
		var arguments = Arguments.argumentsFrom(input);

		assertArrayEquals(new Object[] { "a", 2, null }, arguments.get());
	}

	@Test
	void argumentsFromSupportsIterable() {
		var input = new IterableWithNullableElements("a", 2, null);
		var arguments = Arguments.argumentsFrom(input);

		assertArrayEquals(new Object[] { "a", 2, null }, arguments.get());
	}

	@Test
	void argumentSetSupportsCollection() {
		Collection<@Nullable Object> input = asList("x", null, 42);
		var argumentSet = Arguments.argumentSetFrom("list-test", input);

		assertArrayEquals(new Object[] { "x", null, 42 }, argumentSet.get());
		assertThat(argumentSet.getName()).isEqualTo("list-test");
	}

	@Test
	void argumentSetSupportsIterable() {
		var input = new IterableWithNullableElements("x", null, 42);
		var argumentSet = Arguments.argumentSetFrom("list-test", input);

		assertArrayEquals(new Object[] { "x", null, 42 }, argumentSet.get());
		assertThat(argumentSet.getName()).isEqualTo("list-test");
	}

	@Test
	void toListReturnsMutableListOfArguments() {
		var arguments = Arguments.of("a", 2, null);

		var result = arguments.toList();

		assertThat(result).containsExactly("a", 2, null); // preserves content
		result.add("extra"); // confirms mutability
		assertThat(result).contains("extra");
	}

	@Test
	void toListDoesNotAffectInternalArgumentsState() {
		var arguments = Arguments.of("a", 2, null);

		var result = arguments.toList();
		result.add("extra"); // mutate the returned list

		// Confirm that internal state was not modified
		var freshCopy = arguments.toList();
		assertThat(freshCopy).containsExactly("a", 2, null);
	}

	@Test
	void toListWorksOnEmptyArguments() {
		var arguments = Arguments.of();

		var result = arguments.toList();

		assertThat(result).isEmpty();
		result.add("extra");
		assertThat(result).containsExactly("extra");
	}

	private static final class IterableWithNullableElements implements Iterable<@Nullable Object> {

		private final Collection<@Nullable Object> collection;

		private IterableWithNullableElements(@Nullable Object... items) {
			this.collection = asList(items);
		}

		@Override
		public Iterator<@Nullable Object> iterator() {
			return collection.iterator();
		}
	}

}
