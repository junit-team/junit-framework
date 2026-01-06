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
import java.util.NoSuchElementException;
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
	 * {@code TypeDescriptor} returned when no value is available.
	 */
	public static final TypeDescriptor NONE = new TypeDescriptor(null);

	private final @Nullable Class<?> type;

	public static TypeDescriptor forClass(Class<?> clazz) {
		Preconditions.notNull(clazz, () -> "clazz must not be null");
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

	private TypeDescriptor(@Nullable Class<?> type) {
		this.type = type;
	}

	public Class<?> getType() {
		if (type == null) {
			throw new NoSuchElementException("No type present");
		}
		return type;
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
	// FIXME [NullAway] parameter type of referenced method is @NonNull, but parameter in functional interface method java.util.function.Function.apply(T) is @Nullable
	@SuppressWarnings("NullAway")
	public Optional<Class<?>> getWrapperType() {
		return Optional.ofNullable(type).map(ReflectionUtils::getWrapperType);
	}

	public boolean isPrimitive() {
		return type != null && type.isPrimitive();
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

	@Override
	public String toString() {
		return type != null ? type.getName() : "'null'";
	}

}
