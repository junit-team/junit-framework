/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.util.function.Predicate.isEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link FunctionUtils}.
 *
 * @since 1.0
 */
class FunctionUtilsTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void whereWithNullFunction() {
		var exception = assertThrows(PreconditionViolationException.class, () -> FunctionUtils.where(null, o -> true));
		assertEquals("function must not be null", exception.getMessage());
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void whereWithNullPredicate() {
		var exception = assertThrows(PreconditionViolationException.class, () -> FunctionUtils.where(o -> o, null));
		assertEquals("predicate must not be null", exception.getMessage());
	}

	@Test
	void whereWithChecksPredicateAgainstResultOfFunction() {
		var combinedPredicate = FunctionUtils.where(String::length, isEqual(3));
		assertFalse(combinedPredicate.test("fo"));
		assertTrue(combinedPredicate.test("foo"));
		assertFalse(combinedPredicate.test("fooo"));
	}

}
