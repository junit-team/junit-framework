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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.UniqueId.Segment;

/**
 * @since 1.0
 */
class UniqueIdFormatTests {

	@Nested
	class Formatting {

		private final UniqueId engineId = UniqueId.root("engine", "junit-jupiter");

		private final UniqueIdFormat format = UniqueIdFormat.getDefault();

		@Test
		void engineIdOnly() {
			assertEquals("[engine:junit-jupiter]", engineId.toString());
			assertEquals(format.format(engineId), engineId.toString());
		}

		@Test
		void withTwoSegments() {
			var classId = engineId.append("class", "org.junit.MyClass");
			assertEquals("[engine:junit-jupiter]/[class:org.junit.MyClass]", classId.toString());
			assertEquals(format.format(classId), classId.toString());
		}

		@Test
		void withManySegments() {
			var uniqueId = engineId.append("t1", "v1").append("t2", "v2").append("t3", "v3");
			assertEquals("[engine:junit-jupiter]/[t1:v1]/[t2:v2]/[t3:v3]", uniqueId.toString());
			assertEquals(format.format(uniqueId), uniqueId.toString());
		}

	}

	@Nested
	class ParsingWithDefaultFormat implements ParsingTestTrait {

		private final UniqueIdFormat format = UniqueIdFormat.getDefault();

		@Override
		public UniqueIdFormat getFormat() {
			return this.format;
		}

		@Override
		public String getEngineUid() {
			return "[engine:junit-jupiter]";
		}

		@Override
		public String getDefaultEngineUid() {
			return getEngineUid();
		}

		@Override
		public String getMethodUid() {
			return "[engine:junit-jupiter]/[class:MyClass]/[method:myMethod]";
		}

		@Override
		public String getDefaultMethodUid() {
			return getMethodUid();
		}

	}

	@Nested
	class ParsingWithCustomFormat implements ParsingTestTrait {

		private final UniqueIdFormat format = new UniqueIdFormat('{', '=', '}', ',');

		@Override
		public UniqueIdFormat getFormat() {
			return this.format;
		}

		@Override
		public String getEngineUid() {
			return "{engine=junit-jupiter}";
		}

		@Override
		public String getDefaultEngineUid() {
			return "[engine:junit-jupiter]";
		}

		@Override
		public String getMethodUid() {
			return "{engine=junit-jupiter},{class=MyClass},{method=myMethod}";
		}

		@Override
		public String getDefaultMethodUid() {
			return "[engine:junit-jupiter]/[class:MyClass]/[method:myMethod]";
		}

	}

	// -------------------------------------------------------------------------

	private static void assertSegment(Segment segment, String expectedType, String expectedValue) {
		assertEquals(expectedType, segment.getType(), "segment type");
		assertEquals(expectedValue, segment.getValue(), "segment value");
	}

	interface ParsingTestTrait {

		UniqueIdFormat getFormat();

		String getEngineUid();

		String getDefaultEngineUid();

		String getMethodUid();

		String getDefaultMethodUid();

		@Test
		default void parseMalformedUid() {
			Throwable throwable = assertThrows(JUnitException.class, () -> getFormat().parse("malformed UID"));
			assertThat(throwable).hasMessageContaining("malformed UID");
		}

		@Test
		default void parseEngineUid() {
			var parsedId = getFormat().parse(getEngineUid());
			assertSegment(parsedId.getSegments().getFirst(), "engine", "junit-jupiter");
			assertEquals(getEngineUid(), getFormat().format(parsedId));
			assertEquals(getDefaultEngineUid(), parsedId.toString());
		}

		@Test
		default void parseMethodUid() {
			var parsedId = getFormat().parse(getMethodUid());
			assertSegment(parsedId.getSegments().get(0), "engine", "junit-jupiter");
			assertSegment(parsedId.getSegments().get(1), "class", "MyClass");
			assertSegment(parsedId.getSegments().get(2), "method", "myMethod");
			assertEquals(getMethodUid(), getFormat().format(parsedId));
			assertEquals(getDefaultMethodUid(), parsedId.toString());
		}

	}

}
