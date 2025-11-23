/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apiguardian.api.API;

/**
 * {@code OutputFormat} provides utility methods for formatting collections
 * and other data structures for display in assertion failure messages.
 *
 * <p>This class is primarily used internally by the JUnit Jupiter API
 * to format output in assertion failures, providing clear and readable
 * representations of expected vs actual values.
 *
 * @since 5.11
 */
@API(status = INTERNAL, since = "5.11")
public final class OutputFormat {

	private OutputFormat() {
		/* no-op */
	}

	/**
	 * Creates a supplier that formats two collections vertically side-by-side
	 * for comparison in assertion failure output.
	 *
	 * <p>The formatting displays the expected collection on the left and the
	 * actual collection on the right, with elements aligned for easy comparison.
	 * This format is particularly useful for assertion failures where collections
	 * differ and need to be visually compared.
	 *
	 * <p>Example output format:
	 * <pre>
	 * Expected                  Actual
	 * ========                  ======
	 * [element1]                [element1]
	 * [element2]                [differentElement]
	 * [element3]                [element3]
	 * </pre>
	 *
	 * @param expected the expected collection
	 * @param actual the actual collection
	 * @return a supplier that provides the formatted string when called
	 */
	public static Supplier<String> ofVertical(Collection<?> expected, Collection<?> actual) {
		return () -> {
			if (expected == null && actual == null) {
				return "Expected: null\nActual:   null";
			}
			if (expected == null) {
				return "Expected: null\nActual:   " + formatCollection(actual);
			}
			if (actual == null) {
				return "Expected: " + formatCollection(expected) + "\nActual:   null";
			}

			List<String> expectedStrings = expected.stream()
					.map(String::valueOf)
					.collect(Collectors.toList());
			List<String> actualStrings = actual.stream()
					.map(String::valueOf)
					.collect(Collectors.toList());

			int maxExpectedWidth = expectedStrings.stream()
					.mapToInt(String::length)
					.max()
					.orElse(0);
			maxExpectedWidth = Math.max(maxExpectedWidth, "Expected".length());

			StringBuilder result = new StringBuilder();
			
			// Header line
			result.append(String.format("%-" + maxExpectedWidth + "s  %s%n", "Expected", "Actual"));
			result.append(String.format("%-" + maxExpectedWidth + "s  %s%n", "=".repeat(Math.min(maxExpectedWidth, "Expected".length())), "=".repeat("Actual".length())));

			// Format side-by-side comparison
			int maxSize = Math.max(expectedStrings.size(), actualStrings.size());
			for (int i = 0; i < maxSize; i++) {
				String expectedItem = i < expectedStrings.size() ? "[" + expectedStrings.get(i) + "]" : "";
				String actualItem = i < actualStrings.size() ? "[" + actualStrings.get(i) + "]" : "";
				result.append(String.format("%-" + maxExpectedWidth + "s  %s%n", expectedItem, actualItem));
			}

			return result.toString().trim();
		};
	}

	/**
	 * Formats a collection into a string representation.
	 *
	 * @param collection the collection to format
	 * @return the formatted string representation
	 */
	private static String formatCollection(Collection<?> collection) {
		if (collection == null) {
			return "null";
		}
		if (collection.isEmpty()) {
			return "[]";
		}
		return collection.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(", ", "[", "]"));
	}
}
