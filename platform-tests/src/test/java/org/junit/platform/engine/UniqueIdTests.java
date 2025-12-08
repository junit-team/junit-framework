/*
 * Copyright 2015-2025 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.UniqueId.Segment;

/**
 * Unit tests for {@link UniqueId}.
 *
 * @since 1.0
 * @see org.junit.jupiter.engine.execution.UniqueIdParsingForArrayParameterIntegrationTests
 */
class UniqueIdTests {

	private static final String ENGINE_ID = "junit-jupiter";
	private static final String SUITE_ENGINE_ID = "junit-platform-suite";

	@Nested
	class Creation {

		@Test
		void uniqueIdCanBeCreatedFromEngineId() {
			var uniqueId = UniqueId.forEngine(ENGINE_ID);

			assertEquals("[engine:junit-jupiter]", uniqueId.toString());
			assertSegment(uniqueId.getSegments().getFirst(), "engine", "junit-jupiter");
		}

		@Test
		void engineIdCanBeAppended() {
			var suiteEngineId = UniqueId.forEngine(SUITE_ENGINE_ID);
			var uniqueId = suiteEngineId.appendEngine(ENGINE_ID);
			assertSegment(uniqueId.getSegments().get(1), "engine", "junit-jupiter");
		}

		@Test
		void retrievingOptionalEngineId() {
			var uniqueIdWithEngine = UniqueId.forEngine(ENGINE_ID);
			assertThat(uniqueIdWithEngine.getEngineId()).contains("junit-jupiter");

			var uniqueIdWithoutEngine = UniqueId.root("root", "aValue");
			assertEquals(Optional.empty(), uniqueIdWithoutEngine.getEngineId());
		}

		@Test
		void uniqueIdCanBeCreatedFromTypeAndValue() {
			var uniqueId = UniqueId.root("aType", "aValue");

			assertEquals("[aType:aValue]", uniqueId.toString());
			assertSegment(uniqueId.getSegments().getFirst(), "aType", "aValue");
		}

		@Test
		void rootSegmentCanBeRetrieved() {
			var uniqueId = UniqueId.root("aType", "aValue");

			assertThat(uniqueId.getRoot()).contains(new Segment("aType", "aValue"));
		}

		@Test
		void appendingOneSegment() {
			var engineId = UniqueId.root("engine", ENGINE_ID);
			var classId = engineId.append("class", "org.junit.MyClass");

			assertThat(classId.getSegments()).hasSize(2);
			assertSegment(classId.getSegments().get(0), "engine", ENGINE_ID);
			assertSegment(classId.getSegments().get(1), "class", "org.junit.MyClass");
		}

		@Test
		void appendingSegmentLeavesOriginalUnchanged() {
			var uniqueId = UniqueId.root("engine", ENGINE_ID);
			uniqueId.append("class", "org.junit.MyClass");

			assertThat(uniqueId.getSegments()).hasSize(1);
			assertSegment(uniqueId.getSegments().getFirst(), "engine", ENGINE_ID);
		}

		@Test
		void appendingSeveralSegments() {
			var engineId = UniqueId.root("engine", ENGINE_ID);
			var uniqueId = engineId.append("t1", "v1").append("t2", "v2").append("t3", "v3");

			assertThat(uniqueId.getSegments()).hasSize(4);
			assertSegment(uniqueId.getSegments().get(0), "engine", ENGINE_ID);
			assertSegment(uniqueId.getSegments().get(1), "t1", "v1");
			assertSegment(uniqueId.getSegments().get(2), "t2", "v2");
			assertSegment(uniqueId.getSegments().get(3), "t3", "v3");
		}

		@Test
		void appendingSegmentInstance() {
			var uniqueId = UniqueId.forEngine(ENGINE_ID).append("t1", "v1");

			uniqueId = uniqueId.append(new Segment("t2", "v2"));

			assertThat(uniqueId.getSegments()).hasSize(3);
			assertSegment(uniqueId.getSegments().get(0), "engine", ENGINE_ID);
			assertSegment(uniqueId.getSegments().get(1), "t1", "v1");
			assertSegment(uniqueId.getSegments().get(2), "t2", "v2");
		}

		@SuppressWarnings("DataFlowIssue")
		@Test
		void appendingNullIsNotAllowed() {
			var uniqueId = UniqueId.forEngine(ENGINE_ID);

			assertPreconditionViolationFor(() -> uniqueId.append(null));
			assertPreconditionViolationFor(() -> uniqueId.append(null, "foo"));
			assertPreconditionViolationFor(() -> uniqueId.append("foo", null));
		}

	}

	@Nested
	class ParsingAndFormatting {

		@Test
		void ensureDefaultUniqueIdFormatIsUsedForParsing() {
			var uniqueIdString = "[engine:junit-jupiter]/[class:MyClass]/[method:myMethod]";
			var parsedDirectly = UniqueId.parse(uniqueIdString);
			var parsedViaFormat = UniqueIdFormat.getDefault().parse(uniqueIdString);
			assertEquals(parsedViaFormat, parsedDirectly);
		}

		@Test
		void ensureDefaultUniqueIdFormatIsUsedForFormatting() {
			var parsedDirectly = UniqueId.parse("[engine:junit-jupiter]/[class:MyClass]/[method:myMethod]");
			assertEquals("[engine:junit-jupiter]/[class:MyClass]/[method:myMethod]", parsedDirectly.toString());
		}

		@Test
		void ensureDefaultUniqueIdFormatDecodingEncodesSegmentParts() {
			var segment = UniqueId.parse("[%5B+%25+%5D):(%3A+%2B+%2F]").getSegments().getFirst();
			assertEquals("[ % ])", segment.getType());
			assertEquals("(: + /", segment.getValue());
		}

		@Test
		void ensureDefaultUniqueIdFormatCanHandleAllCharacters() {
			for (char c = 0; c < Character.MAX_VALUE; c++) {
				var value = "foo " + c + " bar";
				var uniqueId = UniqueId.parse(UniqueId.root("type", value).toString());
				var segment = uniqueId.getSegments().getFirst();
				assertEquals(value, segment.getValue());
			}
		}

		@ParameterizedTest
		@ValueSource(strings = { "[a:b]", "[a:b]/[a:b]", "[a$b:b()]", "[a:b(%5BI)]", "[%5B%5D:%3A%2F]" })
		void ensureDefaultToStringAndParsingIsIdempotent(String expected) {
			assertEquals(expected, UniqueId.parse(expected).toString());
		}
	}

	@Nested
	class EqualsContract {

		@Test
		void sameEnginesAreEqual() {
			var id1 = UniqueId.root("engine", "junit-jupiter");
			var id2 = UniqueId.root("engine", "junit-jupiter");

			assertEquals(id2, id1);
			assertEquals(id1, id2);
			assertEquals(id1.hashCode(), id2.hashCode());
		}

		@Test
		void differentEnginesAreNotEqual() {
			var id1 = UniqueId.root("engine", "junit-vintage");
			var id2 = UniqueId.root("engine", "junit-jupiter");

			assertNotEquals(id2, id1);
			assertNotEquals(id1, id2);
		}

		@Test
		void uniqueIdWithSameSegmentsAreEqual() {
			var id1 = UniqueId.root("engine", "junit-jupiter").append("t1", "v1").append("t2", "v2");
			var id2 = UniqueId.root("engine", "junit-jupiter").append("t1", "v1").append("t2", "v2");

			assertEquals(id2, id1);
			assertEquals(id1, id2);
			assertEquals(id1.hashCode(), id2.hashCode());
		}

		@Test
		void differentOrderOfSegmentsAreNotEqual() {
			var id1 = UniqueId.root("engine", "junit-jupiter").append("t2", "v2").append("t1", "v1");
			var id2 = UniqueId.root("engine", "junit-jupiter").append("t1", "v1").append("t2", "v2");

			assertNotEquals(id2, id1);
			assertNotEquals(id1, id2);
		}

		@Test
		void additionalSegmentMakesItNotEqual() {
			var id1 = UniqueId.root("engine", "junit-jupiter").append("t1", "v1");
			var id2 = id1.append("t2", "v2");

			assertNotEquals(id2, id1);
			assertNotEquals(id1, id2);
		}
	}

	@Nested
	class Prefixing {

		@SuppressWarnings("DataFlowIssue")
		@Test
		void nullIsNotAPrefix() {
			var id = UniqueId.forEngine(ENGINE_ID);

			assertPreconditionViolationFor(() -> id.hasPrefix(null));
		}

		@Test
		void uniqueIdIsPrefixForItself() {
			var id = UniqueId.forEngine(ENGINE_ID).append("t1", "v1").append("t2", "v2");

			assertTrue(id.hasPrefix(id));
		}

		@Test
		void uniqueIdIsPrefixForUniqueIdWithAdditionalSegments() {
			var id1 = UniqueId.forEngine(ENGINE_ID);
			var id2 = id1.append("t1", "v1");
			var id3 = id2.append("t2", "v2");

			assertFalse(id1.hasPrefix(id2));
			assertFalse(id1.hasPrefix(id3));
			assertTrue(id2.hasPrefix(id1));
			assertFalse(id2.hasPrefix(id3));
			assertTrue(id3.hasPrefix(id1));
			assertTrue(id3.hasPrefix(id2));
		}

		@Test
		void completelyUnrelatedUniqueIdsAreNotPrefixesForEachOther() {
			var id1 = UniqueId.forEngine("foo");
			var id2 = UniqueId.forEngine("bar");

			assertFalse(id1.hasPrefix(id2));
			assertFalse(id2.hasPrefix(id1));
		}

	}

	@Nested
	class LastSegment {

		@Test
		void returnsLastSegment() {
			var uniqueId = UniqueId.forEngine("foo");
			assertSame(uniqueId.getSegments().get(0), uniqueId.getLastSegment());

			uniqueId = UniqueId.forEngine("foo").append("type", "bar");
			assertSame(uniqueId.getSegments().get(1), uniqueId.getLastSegment());
		}

		@Test
		void removesLastSegment() {
			var uniqueId = UniqueId.forEngine("foo");
			assertPreconditionViolationFor(uniqueId::removeLastSegment);

			var newUniqueId = uniqueId.append("type", "bar").removeLastSegment();
			assertEquals(uniqueId, newUniqueId);
		}

	}

	@Nested
	class Serialization {

		final UniqueId uniqueId = UniqueId.root("engine", "junit-jupiter");

		@Test
		void roundTrip() throws IOException, ClassNotFoundException {
			var bytesOut = new ByteArrayOutputStream();
			var out = new ObjectOutputStream(bytesOut);
			out.writeObject(uniqueId);

			var bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
			var in = new ObjectInputStream(bytesIn);
			var actual = in.readObject();

			assertEquals(uniqueId, actual);
			assertEquals(uniqueId.toString(), actual.toString());
		}

		@Test
		void deserializeFromJunit60() throws IOException, ClassNotFoundException {
			/*
			 Serialized representation of:
			 	new UniqueId(
			 		new UniqueIdFormat('[', ':', ']', '/'),
			 		List.of(new Segment("engine", "junit-jupiter"))
			 );
			 */
			var uniqueIdFromJunit60 = Base64.getMimeDecoder().decode("""
					rO0ABXNyACJvcmcuanVuaXQucGxhdGZvcm0uZW5naW5lLlVuaXF1ZUlkAAAAAAAAAAECAAJMAAhzZWdtZW50c3QAEExqYXZhL3V0
					aWwvTGlzdDtMAA51bmlxdWVJZEZvcm1hdHQAKkxvcmcvanVuaXQvcGxhdGZvcm0vZW5naW5lL1VuaXF1ZUlkRm9ybWF0O3hwc3IA
					EWphdmEudXRpbC5Db2xsU2VyV46rtjobqBEDAAFJAAN0YWd4cAAAAAF3BAAAAAFzcgAqb3JnLmp1bml0LnBsYXRmb3JtLmVuZ2lu
					ZS5VbmlxdWVJZCRTZWdtZW50AAAAAAAAAAECAAJMAAR0eXBldAASTGphdmEvbGFuZy9TdHJpbmc7TAAFdmFsdWVxAH4AB3hwdAAG
					ZW5naW5ldAANanVuaXQtanVwaXRlcnhzcgAob3JnLmp1bml0LnBsYXRmb3JtLmVuZ2luZS5VbmlxdWVJZEZvcm1hdAAAAAAAAAAB
					AgAGQwAMY2xvc2VTZWdtZW50QwALb3BlblNlZ21lbnRDABBzZWdtZW50RGVsaW1pdGVyQwASdHlwZVZhbHVlU2VwYXJhdG9yTAAT
					ZW5jb2RlZENoYXJhY3Rlck1hcHQAE0xqYXZhL3V0aWwvSGFzaE1hcDtMAA5zZWdtZW50UGF0dGVybnQAGUxqYXZhL3V0aWwvcmVn
					ZXgvUGF0dGVybjt4cABdAFsALwA6c3IAEWphdmEudXRpbC5IYXNoTWFwBQfawcMWYNEDAAJGAApsb2FkRmFjdG9ySQAJdGhyZXNo
					b2xkeHA/QAAAAAAADHcIAAAAEAAAAAZzcgATamF2YS5sYW5nLkNoYXJhY3RlcjSLR9lrGiZ4AgABQwAFdmFsdWV4cAAldAADJTI1
					c3EAfgARADp0AAMlM0FzcQB+ABEAW3QAAyU1QnNxAH4AEQArdAADJTJCc3EAfgARAF10AAMlNURzcQB+ABEAL3QAAyUyRnhzcgAX
					amF2YS51dGlsLnJlZ2V4LlBhdHRlcm5GZ9VrbkkCDQIAAkkABWZsYWdzTAAHcGF0dGVybnEAfgAHeHAAAAAgdAAXXFFbXEUoLisp
					XFE6XEUoLispXFFdXEU=""");

			var bytesIn = new ByteArrayInputStream(uniqueIdFromJunit60);
			var in = new ObjectInputStream(bytesIn);
			var actual = in.readObject();

			assertEquals(uniqueId, actual);
			assertEquals(uniqueId.toString(), actual.toString());
		}

		@Test
		void deserializeFromJunit60IgnoresCustomFormat() throws IOException, ClassNotFoundException {
			/*
			  Serialized representation of:
				new UniqueId(
					new UniqueIdFormat('{', '=', '}', ','),
					List.of(new Segment("engine", "junit-jupiter"))
			 );
			 */
			var uniqueIdWithCustomFormatFromJunit60 = Base64.getMimeDecoder().decode("""
					rO0ABXNyACJvcmcuanVuaXQucGxhdGZvcm0uZW5naW5lLlVuaXF1ZUlkAAAAAAAAAAECAAJMAAhzZWdtZW50c3QAEExqYXZhL3V0
					aWwvTGlzdDtMAA51bmlxdWVJZEZvcm1hdHQAKkxvcmcvanVuaXQvcGxhdGZvcm0vZW5naW5lL1VuaXF1ZUlkRm9ybWF0O3hwc3IA
					EWphdmEudXRpbC5Db2xsU2VyV46rtjobqBEDAAFJAAN0YWd4cAAAAAF3BAAAAAFzcgAqb3JnLmp1bml0LnBsYXRmb3JtLmVuZ2lu
					ZS5VbmlxdWVJZCRTZWdtZW50AAAAAAAAAAECAAJMAAR0eXBldAASTGphdmEvbGFuZy9TdHJpbmc7TAAFdmFsdWVxAH4AB3hwdAAG
					ZW5naW5ldAANanVuaXQtanVwaXRlcnhzcgAob3JnLmp1bml0LnBsYXRmb3JtLmVuZ2luZS5VbmlxdWVJZEZvcm1hdAAAAAAAAAAB
					AgAGQwAMY2xvc2VTZWdtZW50QwALb3BlblNlZ21lbnRDABBzZWdtZW50RGVsaW1pdGVyQwASdHlwZVZhbHVlU2VwYXJhdG9yTAAT
					ZW5jb2RlZENoYXJhY3Rlck1hcHQAE0xqYXZhL3V0aWwvSGFzaE1hcDtMAA5zZWdtZW50UGF0dGVybnQAGUxqYXZhL3V0aWwvcmVn
					ZXgvUGF0dGVybjt4cAB9AHsALAA9c3IAEWphdmEudXRpbC5IYXNoTWFwBQfawcMWYNEDAAJGAApsb2FkRmFjdG9ySQAJdGhyZXNo
					b2xkeHA/QAAAAAAADHcIAAAAEAAAAAZzcgATamF2YS5sYW5nLkNoYXJhY3RlcjSLR9lrGiZ4AgABQwAFdmFsdWV4cAAldAADJTI1
					c3EAfgARAHt0AAMlN0JzcQB+ABEAK3QAAyUyQnNxAH4AEQAsdAADJTJDc3EAfgARAH10AAMlN0RzcQB+ABEAPXQAAyUzRHhzcgAX
					amF2YS51dGlsLnJlZ2V4LlBhdHRlcm5GZ9VrbkkCDQIAAkkABWZsYWdzTAAHcGF0dGVybnEAfgAHeHAAAAAgdAAXXFF7XEUoLisp
					XFE9XEUoLispXFF9XEU=""");

			var bytesIn = new ByteArrayInputStream(uniqueIdWithCustomFormatFromJunit60);
			var in = new ObjectInputStream(bytesIn);
			var actual = in.readObject();

			assertEquals(uniqueId, actual);
			assertEquals(uniqueId.toString(), actual.toString());
		}

	}

	private static void assertSegment(Segment segment, String expectedType, String expectedValue) {
		assertEquals(expectedType, segment.getType(), "segment type");
		assertEquals(expectedValue, segment.getValue(), "segment value");
	}

}
