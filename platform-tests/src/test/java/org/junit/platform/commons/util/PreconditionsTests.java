/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.junit.platform.commons.util.Preconditions.condition;
import static org.junit.platform.commons.util.Preconditions.containsNoNullElements;
import static org.junit.platform.commons.util.Preconditions.notBlank;
import static org.junit.platform.commons.util.Preconditions.notEmpty;
import static org.junit.platform.commons.util.Preconditions.notNull;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Preconditions}.
 *
 * @since 1.0
 */
class PreconditionsTests {

	@Test
	void notNullPassesForNonNullObject() {
		var object = new Object();
		var nonNullObject = notNull(object, "message");
		assertSame(object, nonNullObject);
	}

	@Test
	void notNullThrowsForNullObject() {
		var message = "argument is null";

		assertPreconditionViolationFor(() -> notNull(null, message)).withMessage(message);
	}

	@Test
	void notNullThrowsForNullObjectAndMessageSupplier() {
		var message = "argument is null";
		Object object = null;

		assertPreconditionViolationFor(() -> notNull(object, () -> message)).withMessage(message);
	}

	@Test
	void notEmptyPassesForNonEmptyArray() {
		var array = new String[] { "a", "b", "c" };
		var nonEmptyArray = notEmpty(array, () -> "should not fail");
		assertSame(array, nonEmptyArray);
	}

	@Test
	void notEmptyPassesForNonEmptyCollection() {
		Collection<String> collection = List.of("a", "b", "c");
		var nonEmptyCollection = notEmpty(collection, () -> "should not fail");
		assertSame(collection, nonEmptyCollection);
	}

	@Test
	void notEmptyPassesForArrayWithNullElements() {
		notEmpty(new String[] { null }, "message");
	}

	@Test
	void notEmptyPassesForCollectionWithNullElements() {
		notEmpty(singletonList(null), "message");
	}

	@Test
	void notEmptyThrowsForNullArray() {
		var message = "array is empty";

		assertPreconditionViolationFor(() -> notEmpty((Object[]) null, message)).withMessage(message);
	}

	@Test
	void notEmptyThrowsForNullCollection() {
		var message = "collection is empty";

		assertPreconditionViolationFor(() -> notEmpty((Collection<?>) null, message)).withMessage(message);
	}

	@Test
	void notEmptyThrowsForEmptyArray() {
		var message = "array is empty";

		assertPreconditionViolationFor(() -> notEmpty(new Object[0], message)).withMessage(message);
	}

	@Test
	void notEmptyThrowsForEmptyCollection() {
		var message = "collection is empty";

		assertPreconditionViolationFor(() -> notEmpty(List.of(), message)).withMessage(message);
	}

	@Test
	void containsNoNullElementsPassesForArrayThatIsNullOrEmpty() {
		containsNoNullElements((Object[]) null, "array is null");
		containsNoNullElements((Object[]) null, () -> "array is null");

		containsNoNullElements(new Object[0], "array is empty");
		containsNoNullElements(new Object[0], () -> "array is empty");
	}

	@Test
	void containsNoNullElementsPassesForCollectionThatIsNullOrEmpty() {
		containsNoNullElements((List<?>) null, "collection is null");
		containsNoNullElements(List.of(), "collection is empty");

		containsNoNullElements((List<?>) null, () -> "collection is null");
		containsNoNullElements(List.of(), () -> "collection is empty");
	}

	@Test
	void containsNoNullElementsPassesForArrayContainingNonNullElements() {
		var input = new String[] { "a", "b", "c" };
		var output = containsNoNullElements(input, "message");
		assertSame(input, output);
	}

	@Test
	void containsNoNullElementsPassesForCollectionContainingNonNullElements() {
		var input = List.of("a", "b", "c");
		var output = containsNoNullElements(input, "message");
		assertSame(input, output);

		output = containsNoNullElements(input, () -> "message");
		assertSame(input, output);
	}

	@Test
	void containsNoNullElementsThrowsForArrayContainingNullElements() {
		var message = "array contains null elements";
		Object[] array = { new Object(), null, new Object() };

		assertPreconditionViolationFor(() -> containsNoNullElements(array, message)).withMessage(message);
	}

	@Test
	void containsNoNullElementsThrowsForCollectionContainingNullElements() {
		var message = "collection contains null elements";

		assertPreconditionViolationFor(() -> containsNoNullElements(singletonList(null), message)).withMessage(message);
	}

	@Test
	void notBlankPassesForNonBlankString() {
		var string = "abc";
		var nonBlankString = notBlank(string, "message");
		assertSame(string, nonBlankString);
	}

	@Test
	void notBlankThrowsForNullString() {
		var message = "string shouldn't be blank";

		assertPreconditionViolationFor(() -> notBlank(null, message)).withMessage(message);
	}

	@Test
	void notBlankThrowsForNullStringWithMessageSupplier() {
		var message = "string shouldn't be blank";

		assertPreconditionViolationFor(() -> notBlank(null, () -> message)).withMessage(message);
	}

	@Test
	void notBlankThrowsForEmptyString() {
		var message = "string shouldn't be blank";

		assertPreconditionViolationFor(() -> notBlank("", message)).withMessage(message);
	}

	@Test
	void notBlankThrowsForEmptyStringWithMessageSupplier() {
		var message = "string shouldn't be blank";

		assertPreconditionViolationFor(() -> notBlank("", () -> message)).withMessage(message);
	}

	@Test
	void notBlankThrowsForBlankString() {
		var message = "string shouldn't be blank";

		assertPreconditionViolationFor(() -> notBlank("          ", message)).withMessage(message);
	}

	@Test
	void notBlankThrowsForBlankStringWithMessageSupplier() {
		var message = "string shouldn't be blank";

		assertPreconditionViolationFor(() -> notBlank("          ", () -> message)).withMessage(message);
	}

	@Test
	void conditionPassesForTruePredicate() {
		condition(true, "error message");
	}

	@Test
	void conditionPassesForTruePredicateWithMessageSupplier() {
		condition(true, () -> "error message");
	}

	@Test
	void conditionThrowsForFalsePredicate() {
		var message = "condition does not hold";

		assertPreconditionViolationFor(() -> condition(false, message)).withMessage(message);
	}

	@Test
	void conditionThrowsForFalsePredicateWithMessageSupplier() {
		var message = "condition does not hold";

		assertPreconditionViolationFor(() -> condition(false, () -> message)).withMessage(message);
	}

}
