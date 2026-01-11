/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.SerializationUtils.deserialize;
import static org.junit.platform.commons.util.SerializationUtils.serialize;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.IntStream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.fakes.TestDescriptorStub;

/**
 * @since 1.0
 */
@NullMarked
class TestIdentifierTests {

	@Test
	void inheritsIdAndNamesFromDescriptor() {
		TestDescriptor testDescriptor = new TestDescriptorStub(UniqueId.root("aType", "uniqueId"), "displayName");
		var testIdentifier = TestIdentifier.from(testDescriptor);

		assertEquals("[aType:uniqueId]", testIdentifier.getUniqueId());
		assertEquals("displayName", testIdentifier.getDisplayName());
	}

	@Test
	void inheritsTypeFromDescriptor() {
		TestDescriptor descriptor = new TestDescriptorStub(UniqueId.root("aType", "uniqueId"), "displayName");
		var identifier = TestIdentifier.from(descriptor);
		assertEquals(TestDescriptor.Type.TEST, identifier.getType());
		assertTrue(identifier.isTest());
		assertFalse(identifier.isContainer());

		descriptor.addChild(new TestDescriptorStub(UniqueId.root("aChild", "uniqueId"), "displayName"));
		identifier = TestIdentifier.from(descriptor);
		assertEquals(TestDescriptor.Type.CONTAINER, identifier.getType());
		assertFalse(identifier.isTest());
		assertTrue(identifier.isContainer());
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 2 })
	void currentVersionCanBeSerializedAndDeserialized(int tagCount) throws Exception {
		var tags = IntStream.range(0, tagCount) //
				.mapToObj(i -> TestTag.create("tag-" + i)) //
				.collect(collectingAndThen(toSet(), TestIdentifierTests::unserializableSet));

		var original = createOriginalTestIdentifier(tags);

		byte[] bytes = serialize(original);
		var roundTripped = (TestIdentifier) deserialize(bytes);

		assertDeepEquals(original, roundTripped);
		assertThat(original.getTags()).isInstanceOf(Serializable.class);
	}

	private static <T> Set<T> unserializableSet(Set<T> delegate) {
		var wrapper = new AbstractSet<T>() {

			@Override
			public Iterator<T> iterator() {
				return delegate.iterator();
			}

			@Override
			public int size() {
				return delegate.size();
			}
		};
		assertThat(wrapper).isNotInstanceOf(Serializable.class);
		return wrapper;
	}

	@Test
	void identifierWithNoParentCanBeSerializedAndDeserialized() throws Exception {
		TestIdentifier originalIdentifier = TestIdentifier.from(
			new AbstractTestDescriptor(UniqueId.root("example", "id"), "Example") {
				@Override
				public Type getType() {
					return Type.CONTAINER;
				}
			});

		var deserializedIdentifier = (TestIdentifier) deserialize(serialize(originalIdentifier));

		assertDeepEquals(originalIdentifier, deserializedIdentifier);
	}

	private static void assertDeepEquals(TestIdentifier first, TestIdentifier second) {
		assertEquals(first, second);
		assertEquals(first.getUniqueId(), second.getUniqueId());
		assertEquals(first.getUniqueIdObject(), second.getUniqueIdObject());
		assertEquals(first.getDisplayName(), second.getDisplayName());
		assertEquals(first.getLegacyReportingName(), second.getLegacyReportingName());
		assertEquals(first.getSource(), second.getSource());
		assertEquals(first.getTags(), second.getTags());
		assertEquals(first.getType(), second.getType());
		assertEquals(first.getParentId(), second.getParentId());
		assertEquals(first.getParentIdObject(), second.getParentIdObject());
	}

	private static TestIdentifier createOriginalTestIdentifier(Set<TestTag> tags) {
		var engineDescriptor = new EngineDescriptor(UniqueId.forEngine("engine"), "Engine");
		var uniqueId = engineDescriptor.getUniqueId().append("child", "child");
		var testSource = ClassSource.from(TestIdentifierTests.class);

		var testDescriptor = new AbstractTestDescriptor(uniqueId, "displayName", testSource) {
			@Override
			public Type getType() {
				return Type.TEST;
			}

			@Override
			public String getLegacyReportingName() {
				return "reportingName";
			}

			@Override
			public Set<TestTag> getTags() {
				return tags;
			}
		};

		engineDescriptor.addChild(testDescriptor);
		return TestIdentifier.from(testDescriptor);
	}

}
