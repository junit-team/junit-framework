/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ToStringBuilder}.
 *
 * @since 1.0
 */
class ToStringBuilderTests {

	@SuppressWarnings("DataFlowIssue")
	@Test
	void withNullObject() {
		assertPreconditionViolationFor(() -> new ToStringBuilder((Object) null));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void withNullClass() {
		assertPreconditionViolationFor(() -> new ToStringBuilder((Class<?>) null));
	}

	@SuppressWarnings("DataFlowIssue")
	@Test
	void appendWithIllegalName() {
		var builder = new ToStringBuilder("");

		assertPreconditionViolationFor(() -> builder.append(null, ""));
		assertPreconditionViolationFor(() -> builder.append("", ""));
		assertPreconditionViolationFor(() -> builder.append("    ", ""));
	}

	@Test
	void withZeroFields() {
		assertEquals("RoleModel []", new ToStringBuilder(new RoleModel()).toString());
		assertEquals("RoleModel []", new ToStringBuilder(RoleModel.class).toString());
	}

	@Test
	void withOneField() {
		assertEquals("RoleModel [name = 'Dilbert']",
			new ToStringBuilder(new RoleModel()).append("name", "Dilbert").toString());
	}

	@Test
	void withNullField() {
		assertEquals("RoleModel [name = null]", new ToStringBuilder(new RoleModel()).append("name", null).toString());
	}

	@Test
	void withTwoFields() {
		assertEquals("RoleModel [name = 'Dilbert', age = 42]",
			new ToStringBuilder(new RoleModel()).append("name", "Dilbert").append("age", 42).toString());
	}

	@Test
	void withIntegerArrayField() {
		assertEquals("RoleModel [magic numbers = [1, 42, 99]]",
			new ToStringBuilder(new RoleModel()).append("magic numbers", new Integer[] { 1, 42, 99 }).toString());
	}

	@Test
	void withIntArrayField() {
		assertEquals("RoleModel [magic numbers = [1, 42, 23]]",
			new ToStringBuilder(new RoleModel()).append("magic numbers", new int[] { 1, 42, 23 }).toString());
	}

	@Test
	void withCharArrayField() {
		assertEquals("RoleModel [magic characters = [a, b]]",
			new ToStringBuilder(new RoleModel()).append("magic characters", new char[] { 'a', 'b' }).toString());
	}

	@Test
	void withPrimitiveBooleanArrayField() {
		assertEquals("RoleModel [booleans = [true, false, true]]",
			new ToStringBuilder(new RoleModel()).append("booleans", new boolean[] { true, false, true }).toString());
	}

	@Test
	void withShortArrayField() {
		assertEquals("RoleModel [values = [23, 42]]",
			new ToStringBuilder(new RoleModel()).append("values", new short[] { 23, 42 }).toString());
	}

	@Test
	void withByteArrayField() {
		assertEquals("RoleModel [values = [23, 42]]",
			new ToStringBuilder(new RoleModel()).append("values", new byte[] { 23, 42 }).toString());
	}

	@Test
	void withPrimitiveLongArrayField() {
		assertEquals("RoleModel [values = [23, 42]]",
			new ToStringBuilder(new RoleModel()).append("values", new long[] { 23, 42 }).toString());
	}

	@Test
	void withPrimitiveFloatArrayField() {
		assertEquals("RoleModel [values = [23.45, 17.13]]",
			new ToStringBuilder(new RoleModel()).append("values", new float[] { 23.45f, 17.13f }).toString());
	}

	@Test
	void withPrimitiveDoubleArrayField() {
		assertEquals("RoleModel [values = [23.45, 17.13]]",
			new ToStringBuilder(new RoleModel()).append("values", new double[] { 23.45d, 17.13d }).toString());
	}

	@Test
	@SuppressWarnings("serial")
	void withMapField() {
		// @formatter:off
		Map<String,Object> map = new LinkedHashMap<>() {{
			put("foo", 42);
			put("bar", "enigma");
		}};
		// @formatter:on
		assertEquals("RoleModel [mystery map = {foo=42, bar=enigma}]",
			new ToStringBuilder(new RoleModel()).append("mystery map", map).toString());
	}

	@Test
	void withDemoImplementation() {
		var roleModel = new RoleModel("Dilbert", 42);
		assertEquals("RoleModel [name = 'Dilbert', age = 42]", roleModel.toString());
	}

	@NullUnmarked
	static class RoleModel {

		String name;
		int age;

		RoleModel() {
		}

		RoleModel(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@Override
		public String toString() {
			// @formatter:off
			return new ToStringBuilder(this)
				.append("name", this.name)
				.append("age", this.age)
				.toString();
			// @formatter:on
		}
	}

}
