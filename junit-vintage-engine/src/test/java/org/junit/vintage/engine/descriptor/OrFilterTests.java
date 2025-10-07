/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationNotEmptyFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * @since 5.5
 */
class OrFilterTests {

	@Test
	void exceptionWithoutAnyFilters() {
		assertPreconditionViolationNotEmptyFor("filters", () -> new OrFilter(Set.of()));
	}

	@Test
	void evaluatesSingleFilter() {
		var filter = mockFilter("foo", true);

		var orFilter = new OrFilter(Set.of(filter));

		assertEquals("foo", orFilter.describe());

		var description = Description.createTestDescription(getClass(), "evaluatesSingleFilter");
		assertTrue(orFilter.shouldRun(description));

		verify(filter).shouldRun(same(description));
	}

	@Test
	void evaluatesMultipleFilters() {
		var filter1 = mockFilter("foo", false);
		var filter2 = mockFilter("bar", true);

		var orFilter = new OrFilter(List.of(filter1, filter2));

		assertEquals("foo OR bar", orFilter.describe());

		var description = Description.createTestDescription(getClass(), "evaluatesMultipleFilters");
		assertTrue(orFilter.shouldRun(description));

		verify(filter1).shouldRun(same(description));
		verify(filter2).shouldRun(same(description));
	}

	private Filter mockFilter(String description, boolean result) {
		var filter = mock(Filter.class);
		when(filter.describe()).thenReturn(description);
		when(filter.shouldRun(any())).thenReturn(result);
		return filter;
	}

}
