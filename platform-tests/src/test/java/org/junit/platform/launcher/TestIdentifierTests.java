/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

import org.junit.jupiter.api.Test;
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

	@Test
	void currentVersionCanBeSerializedAndDeserialized() throws Exception {
		var originalIdentifier = createOriginalTestIdentifier();
		var uniqueId = originalIdentifier.getUniqueIdObject();
		var testSource = ClassSource.from(TestIdentifierTests.class);

		var recreatedDescriptor = new AbstractTestDescriptor(uniqueId, originalIdentifier.getDisplayName(),
			testSource) {
			@Override
			public Type getType() {
				return Type.TEST;
			}

			@Override
			public String getLegacyReportingName() {
				return originalIdentifier.getLegacyReportingName();
			}

			@Override
			public Set<TestTag> getTags() {
				return Set.of(TestTag.create("aTag"));
			}
		};
		var engineDescriptor = new EngineDescriptor(UniqueId.forEngine("engine"), "Engine");
		engineDescriptor.addChild(recreatedDescriptor);

		var rebuiltIdentifier = TestIdentifier.from(recreatedDescriptor);
		assertDeepEquals(originalIdentifier, rebuiltIdentifier);
	}

	@Test
	void initialVersionCanBeDeserialized() throws Exception {
		var expected = createOriginalTestIdentifier();
		var testSource = ClassSource.from(TestIdentifierTests.class);
		var descriptor = new AbstractTestDescriptor(expected.getUniqueIdObject(), "displayName", testSource) {
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
				return Set.of(TestTag.create("aTag"));
			}
		};
		var engineDescriptor = new EngineDescriptor(UniqueId.forEngine("engine"), "Engine");
		engineDescriptor.addChild(descriptor);

		var actual = TestIdentifier.from(descriptor);

		assertDeepEquals(expected, actual);
	}

	@Test
	void identifierWithNoParentCanBeSerializedAndDeserialized() throws Exception {
		TestIdentifier first = TestIdentifier.from(
			new AbstractTestDescriptor(UniqueId.root("example", "id"), "Example") {
				@Override
				public Type getType() {
					return Type.CONTAINER;
				}
			});

		TestIdentifier second = TestIdentifier.from(
			new AbstractTestDescriptor(UniqueId.root("example", "id"), "Example") {
				@Override
				public Type getType() {
					return Type.CONTAINER;
				}
			});

		assertDeepEquals(first, second);
		assertTrue(first.getParentId().isEmpty());
		assertTrue(second.getParentId().isEmpty());
	}

	/**
	 * Negative test case: Verify that TestIdentifier does not implement Serializable
	 * and attempting to use default Java serialization results in NotSerializableException.
	 */
	@SuppressWarnings("ConstantConditions")
	@Test
	void serializationIsNotSupported() {
		assertFalse(Serializable.class.isAssignableFrom(TestIdentifier.class),
			"TestIdentifier must not implement java.io.Serializable");
		var identifier = createOriginalTestIdentifier();

		assertThrows(NotSerializableException.class, () -> {
			try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
				oos.writeObject(identifier);
			}
		});
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

	private static TestIdentifier createOriginalTestIdentifier() {
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
				return Set.of(TestTag.create("aTag"));
			}
		};
		engineDescriptor.addChild(testDescriptor);
		return TestIdentifier.from(testDescriptor);
	}

}
