/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support.conversion;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 *
 *
 * @since 6.0
 */
@API(status = EXPERIMENTAL, since = "6.0")
public final class TypeDescriptor {

	/**
	 * Internal marker for descriptors created from a {@code null} value.
	 */
	private static final Class<?> NULL_TYPE = Void.class;

	/**
	 * {@code TypeDescriptor} returned when no value is available.
	 */
	public static final TypeDescriptor NONE = new TypeDescriptor(NULL_TYPE);

	private final Class<?> type;

	public static TypeDescriptor forClass(Class<?> clazz) {
		Preconditions.condition(clazz != NULL_TYPE, () -> "clazz must not be " + NULL_TYPE);
		return new TypeDescriptor(clazz);
	}

	public static TypeDescriptor forInstance(@Nullable Object instance) {
		return instance != null ? forClass(instance.getClass()) : NONE;
	}

	public static TypeDescriptor forField(Field field) {
		Preconditions.notNull(field, "field must not be null");
		return forClass(field.getType());
	}

	public static TypeDescriptor forParameter(Parameter parameter) {
		Preconditions.notNull(parameter, "parameter must not be null");
		return forClass(parameter.getType());
	}

	private TypeDescriptor(Class<?> type) {
		this.type = type;
	}

	public @Nullable Class<?> getType() {
		return this != NONE ? type : null;
	}

	/**
	 * Get the wrapper type of this type descriptor, if available.
	 *
	 * <p>If this type descriptor represents a primitive type, this method
	 * returns the corresponding wrapped type. Otherwise, this method returns
	 * {@link Optional#empty() empty()}.
	 *
	 * @return an {@code Optional} containing the wrapper type; never
	 * {@code null} but potentially empty
	 */
	public Optional<Class<?>> getWrapperType() {
		return Optional.ofNullable(ReflectionUtils.getWrapperType(type));
	}

	public boolean isPrimitive() {
		return type.isPrimitive();
	}

	public String getTypeName() {
		return type.getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TypeDescriptor that = (TypeDescriptor) o;
		return Objects.equals(this.type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(type);
	}

}
