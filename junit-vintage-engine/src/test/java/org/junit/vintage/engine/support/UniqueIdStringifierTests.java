/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * @since 4.12
 */
class UniqueIdStringifierTests {

	@Test
	void returnsReadableStringForKnownTypes() {
		var stringifier = new UniqueIdStringifier();

		assertEquals("foo", stringifier.apply("foo"));
		assertEquals("42", stringifier.apply(42));
		assertEquals("42", stringifier.apply(42L));
		assertEquals("42.23", stringifier.apply(42.23d));
	}

	@Test
	void serializesUnknownTypes() throws Exception {
		var stringifier = new UniqueIdStringifier();

		var serialized = stringifier.apply(new MyCustomId(42));

		var deserializedObject = deserialize(decodeBase64(serialized));
		assertThat(deserializedObject).isInstanceOf(MyCustomId.class);
		assertEquals(42, ((MyCustomId) deserializedObject).value());
	}

	@Test
	void usesToStringWhenSerializationFails() {
		var stringifier = new UniqueIdStringifier();
		var serialized = stringifier.apply(new ClassWithErroneousSerialization());

		var deserializedString = new String(decodeBase64(serialized), UniqueIdStringifier.CHARSET);

		assertEquals("value from toString()", deserializedString);
	}

	private byte[] decodeBase64(String value) {
		return Base64.getDecoder().decode(value.getBytes(UniqueIdStringifier.CHARSET));
	}

	private Object deserialize(byte[] bytes) throws Exception {
		try (var inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return inputStream.readObject();
		}
	}

	private record MyCustomId(int value) implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

	}

	private static class ClassWithErroneousSerialization implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		@Serial
		Object writeReplace() throws ObjectStreamException {
			throw new InvalidObjectException("failed on purpose");
		}

		@Override
		public String toString() {
			return "value from toString()";
		}
	}

}
