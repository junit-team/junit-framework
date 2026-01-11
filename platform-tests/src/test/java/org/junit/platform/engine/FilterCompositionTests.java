/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.included;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.launcher.FilterStub;

/**
 * Unit tests for {@link Filter#composeFilters}.
 *
 * {@link Filter#composeFilters} will delegate to {@linkplain CompositeFilter} under the hood.
 *
 * @since 1.0
 */
class FilterCompositionTests {

	@Test
	void composingNoFiltersCreatesFilterThatIncludesEverything() {
		var composedFilter = Filter.composeFilters();

		assertTrue(composedFilter.apply(String.class).included());
		assertTrue(composedFilter.toPredicate().test(String.class));
		assertTrue(composedFilter.apply(Object.class).included());
		assertTrue(composedFilter.toPredicate().test(Object.class));
	}

	@Test
	void composingSingleFilterWillReturnTheOriginalOne() {
		Filter<?> singleFilter = ClassNameFilter.includeClassNamePatterns(".*ring.*");
		var composed = Filter.composeFilters(singleFilter);
		assertSame(singleFilter, composed);
	}

	@Test
	void composingMultipleFiltersIsAConjunctionOfFilters() {
		Filter<String> firstFilter = ClassNameFilter.includeClassNamePatterns(".*ring.*");
		Filter<String> secondFilter = ClassNameFilter.includeClassNamePatterns(".*Join.*");

		var composed = Filter.composeFilters(firstFilter, secondFilter);

		assertFalse(composed.apply("java.lang.String").included());
		assertFalse(composed.toPredicate().test("java.lang.String"));
		assertTrue(composed.apply("java.util.StringJoiner").included());
		assertTrue(composed.toPredicate().test("java.util.StringJoiner"));
	}

	@Test
	void aFilterComposedOfMultipleFiltersHasReadableDescription() {
		Filter<Object> firstFilter = new FilterStub<>(o -> excluded("wrong"), () -> "1st");
		Filter<Object> secondFilter = new FilterStub<>(o -> included("right"), () -> "2nd");

		var composed = Filter.composeFilters(firstFilter, secondFilter);

		assertFalse(composed.apply(String.class).included());
		assertEquals("(1st) and (2nd)", composed.toString());
	}

}
