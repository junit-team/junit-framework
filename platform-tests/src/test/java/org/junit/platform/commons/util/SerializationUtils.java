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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializationUtils {

	public static Object deserialize(byte[] bytes) throws Exception {
		try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return in.readObject();
		}
	}

	public static byte[] serialize(Object object) throws Exception {
		try (var byteArrayOutputStream = new ByteArrayOutputStream();
				var objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
			objectOutputStream.writeObject(object);
			objectOutputStream.flush();
			return byteArrayOutputStream.toByteArray();
		}
	}
}
