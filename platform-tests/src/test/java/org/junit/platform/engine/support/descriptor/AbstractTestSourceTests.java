/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.TestSource;

/**
 * Abstract base class for unit tests involving {@link TestSource TestSources}
 * and {@link FilePosition FilePositions}.
 *
 * @since 1.0
 */
abstract class AbstractTestSourceTests {

	abstract Stream<? extends Serializable> createSerializableInstances() throws Exception;

	@TestFactory
	Stream<DynamicTest> assertToString() throws Exception {
		return createSerializableInstances() //
				.map(instance -> dynamicTest(instance.toString(), () -> assertToString(instance)));
	}

	private void assertToString(Object instance) {
		assertNotNull(instance);
		assertTrue(instance.toString().startsWith(instance.getClass().getSimpleName()));
	}

	@TestFactory
	Stream<DynamicTest> assertSerializable() throws Exception {
		return createSerializableInstances() //
				.map(instance -> dynamicTest(instance.toString(), () -> assertSerializable(instance)));
	}

	private <T extends Serializable> void assertSerializable(T instance) {
		try {
			Class<?> type = instance.getClass();
			var serialized = serialize(instance);
			var deserialized = deserialize(serialized);

			assertTrue(type.isAssignableFrom(deserialized.getClass()));
			assertEquals(instance, deserialized);
		}
		catch (Exception e) {
			fail("assertSerializable failed: " + instance, e);
		}
	}

	private byte[] serialize(Object obj) throws Exception {
		var b = new ByteArrayOutputStream();
		var o = new ObjectOutputStream(b);
		o.writeObject(obj);
		return b.toByteArray();
	}

	private Object deserialize(byte[] bytes) throws Exception {
		var b = new ByteArrayInputStream(bytes);
		var o = new ObjectInputStream(b);
		return o.readObject();
	}

}
