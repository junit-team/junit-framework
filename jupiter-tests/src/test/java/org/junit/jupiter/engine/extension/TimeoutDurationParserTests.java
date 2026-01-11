/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * @since 5.5
 */
class TimeoutDurationParserTests {

	private final TimeoutDurationParser parser = new TimeoutDurationParser();

	@Test
	void parsesNumberWithoutUnitIntoSecondsDurations() {
		assertEquals(new TimeoutDuration(42, SECONDS), parser.parse("42"));
	}

	@TestFactory
	Stream<DynamicNode> parsesNumbersWithUnits() {
		var unitsWithRepresentations = Map.of( //
			NANOSECONDS, "ns", //
			MICROSECONDS, "μs", //
			MILLISECONDS, "ms", //
			SECONDS, "s", //
			MINUTES, "m", //
			HOURS, "h", //
			DAYS, "d");
		return unitsWithRepresentations.entrySet().stream() //
				.map(entry -> {
					var unit = entry.getKey();
					var plainRepresentation = entry.getValue();
					var representations = Stream.of( //
						plainRepresentation, //
						" " + plainRepresentation, //
						plainRepresentation.toUpperCase(), //
						" " + plainRepresentation.toUpperCase());
					return dynamicContainer(unit.name().toLowerCase(),
						representations.map(representation -> dynamicTest("\"" + representation + "\"", () -> {
							var expected = new TimeoutDuration(42, unit);
							var actual = parser.parse("42" + representation);
							assertEquals(expected, actual);
						})));
				});
	}

	@Test
	void rejectsNumbersStartingWithZero() {
		assertThrows(DateTimeParseException.class, () -> parser.parse("01"));
	}
}
