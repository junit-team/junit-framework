/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.aggregator;

import static org.apiguardian.api.API.Status.STABLE;

import java.util.List;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

/**
 * {@code ArgumentsAccessor} defines the public API for accessing arguments provided
 * by an {@link org.junit.jupiter.params.provider.ArgumentsProvider ArgumentsProvider}
 * for a single invocation of a
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass} or
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}.
 *
 * <p>Specifically, an {@code ArgumentsAccessor} <em>aggregates</em> a set of
 * arguments for a given invocation of a parameterized class or parameterized
 * test and provides convenience methods for accessing those arguments in a
 * type-safe manner with support for automatic type conversion.
 *
 * <p>An instance of {@code ArgumentsAccessor} will be automatically supplied
 * for any parameter of type {@code ArgumentsAccessor} in a parameterized class
 * or parameterized test. In addition, {@link ArgumentsAggregator} implementations
 * are given access to an {@code ArgumentsAccessor}.
 *
 * <p>This interface is not intended to be implemented by clients.
 *
 * <p>Additional <a href="https://kotlinlang.org/">Kotlin</a> arguments accessors can be
 * found as <em>extension functions</em> in the {@link org.junit.jupiter.params.aggregator}
 * package.
 *
 * @since 5.2
 * @see ArgumentsAggregator
 * @see org.junit.jupiter.params.ParameterizedTest
 */
@API(status = STABLE, since = "5.7")
public interface ArgumentsAccessor {

	/**
	 * Get the value of the argument at the given index as an {@link Object}.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 */
	@Nullable
	Object get(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as an instance of the
	 * required type.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @param requiredType the required type of the value; never {@code null}
	 * @return the value at the given index, potentially {@code null}
	 */
	<T> @Nullable T get(int index, Class<T> requiredType) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link Character},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	Character getCharacter(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link Boolean},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	Boolean getBoolean(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link Byte},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	Byte getByte(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link Short},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	Short getShort(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link Integer},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	Integer getInteger(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link Long},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	Long getLong(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link Float},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	Float getFloat(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link Double},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	Double getDouble(int index) throws ArgumentAccessException;

	/**
	 * Get the value of the argument at the given index as a {@link String},
	 * performing automatic type conversion as necessary.
	 *
	 * @param index the index of the argument to get; must be greater than or
	 * equal to zero and less than {@link #size}
	 * @return the value at the given index, potentially {@code null}
	 * @throws ArgumentAccessException if the value cannot be accessed
	 * or converted to the desired type
	 */
	@Nullable
	String getString(int index) throws ArgumentAccessException;

	/**
	 * Get the number of arguments in this accessor.
	 */
	int size();

	/**
	 * Get all arguments in this accessor as an array.
	 */
	@Nullable
	Object[] toArray();

	/**
	 * Get all arguments in this accessor as an immutable list.
	 */
	List<@Nullable Object> toList();

	/**
	 * Get the index of the current invocation.
	 */
	int getInvocationIndex();

}
