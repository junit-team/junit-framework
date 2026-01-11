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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.commons.util.ClassUtils.nullSafeToString;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ClassUtils}.
 *
 * @since 1.0
 */
class ClassUtilsTests {

	@Test
	void nullSafeToStringWithDefaultMapper() {
		assertEquals("", nullSafeToString((Class<?>[]) null));
		assertEquals("", nullSafeToString());
		assertEquals("java.lang.String", nullSafeToString(String.class));
		assertEquals("java.lang.String, java.lang.Integer", nullSafeToString(String.class, Integer.class));
		assertEquals("java.lang.String, null, java.lang.Integer", nullSafeToString(String.class, null, Integer.class));
	}

	@Test
	void nullSafeToStringWithCustomMapper() {
		assertEquals("", nullSafeToString(Class::getSimpleName, (Class<?>[]) null));
		assertEquals("", nullSafeToString(Class::getSimpleName));
		assertEquals("String", nullSafeToString(Class::getSimpleName, String.class));
		assertEquals("String, Integer", nullSafeToString(Class::getSimpleName, String.class, Integer.class));
		assertEquals("String, null, Integer",
			nullSafeToString(Class::getSimpleName, String.class, null, Integer.class));
	}

}
