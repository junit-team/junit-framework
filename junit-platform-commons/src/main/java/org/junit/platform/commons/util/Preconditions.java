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

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.annotation.Contract;

/**
 * Collection of utilities for asserting preconditions for method and
 * constructor arguments.
 *
 * <p>Each method in this class throws a {@link PreconditionViolationException}
 * if the precondition is violated.
 *
 * <h2>DISCLAIMER</h2>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class Preconditions {

	private Preconditions() {
		/* no-op */
	}

	/**
	 * Assert that the supplied {@link Object} is not {@code null}.
	 *
	 * @param object the object to check
	 * @param message precondition violation message
	 * @return the supplied object as a convenience
	 * @throws PreconditionViolationException if the supplied object is {@code null}
	 * @see #notNull(Object, Supplier)
	 */
	@Contract("null, _ -> fail; !null, _ -> param1")
	public static <T> T notNull(@Nullable T object, String message) throws PreconditionViolationException {
		condition(object != null, message);
		return object;
	}

	/**
	 * Assert that the supplied {@link Object} is not {@code null}.
	 *
	 * @param object the object to check
	 * @param messageSupplier precondition violation message supplier
	 * @return the supplied object as a convenience
	 * @throws PreconditionViolationException if the supplied object is {@code null}
	 * @see #condition(boolean, Supplier)
	 */
	@Contract("null, _ -> fail; !null, _ -> param1")
	public static <T> T notNull(@Nullable T object, Supplier<String> messageSupplier)
			throws PreconditionViolationException {

		condition(object != null, messageSupplier);
		return object;
	}

	/**
	 * Assert that the supplied array is neither {@code null} nor <em>empty</em>.
	 *
	 * @param array the array to check
	 * @param message precondition violation message
	 * @return the supplied array as a convenience
	 * @throws PreconditionViolationException if the supplied array is
	 * {@code null} or <em>empty</em>
	 * @since 1.9
	 * @see #condition(boolean, String)
	 */
	@Contract("null, _ -> fail")
	@API(status = INTERNAL, since = "1.11")
	public static int[] notEmpty(int @Nullable [] array, String message) throws PreconditionViolationException {
		condition(array != null && array.length > 0, message);
		return array;
	}

	/**
	 * Assert that the supplied array is neither {@code null} nor <em>empty</em>.
	 *
	 * <p><strong>WARNING</strong>: this method does NOT check if the supplied
	 * array contains any {@code null} elements.
	 *
	 * @param array the array to check
	 * @param message precondition violation message
	 * @return the supplied array as a convenience
	 * @throws PreconditionViolationException if the supplied array is
	 * {@code null} or <em>empty</em>
	 * @see #containsNoNullElements(Object[], String)
	 * @see #condition(boolean, String)
	 */
	@Contract("null, _ -> fail")
	public static <T> T[] notEmpty(T @Nullable [] array, String message) throws PreconditionViolationException {
		condition(array != null && array.length > 0, message);
		return array;
	}

	/**
	 * Assert that the supplied array is neither {@code null} nor <em>empty</em>.
	 *
	 * <p><strong>WARNING</strong>: this method does NOT check if the supplied
	 * array contains any {@code null} elements.
	 *
	 * @param array the array to check
	 * @param messageSupplier precondition violation message supplier
	 * @return the supplied array as a convenience
	 * @throws PreconditionViolationException if the supplied array is
	 * {@code null} or <em>empty</em>
	 * @see #containsNoNullElements(Object[], String)
	 * @see #condition(boolean, String)
	 */
	@Contract("null, _ -> fail")
	public static <T> T[] notEmpty(T @Nullable [] array, Supplier<String> messageSupplier)
			throws PreconditionViolationException {

		condition(array != null && array.length > 0, messageSupplier);
		return array;
	}

	/**
	 * Assert that the supplied {@link Collection} is neither {@code null} nor empty.
	 *
	 * <p><strong>WARNING</strong>: this method does NOT check if the supplied
	 * collection contains any {@code null} elements.
	 *
	 * @param collection the collection to check
	 * @param message precondition violation message
	 * @return the supplied collection as a convenience
	 * @throws PreconditionViolationException if the supplied collection is {@code null} or empty
	 * @see #containsNoNullElements(Collection, String)
	 * @see #condition(boolean, String)
	 */
	@Contract("null, _ -> fail")
	public static <T extends Collection<?>> T notEmpty(@Nullable T collection, String message)
			throws PreconditionViolationException {

		condition(collection != null && !collection.isEmpty(), message);
		return collection;
	}

	/**
	 * Assert that the supplied {@link Collection} is neither {@code null} nor empty.
	 *
	 * <p><strong>WARNING</strong>: this method does NOT check if the supplied
	 * collection contains any {@code null} elements.
	 *
	 * @param collection the collection to check
	 * @param messageSupplier precondition violation message supplier
	 * @return the supplied collection as a convenience
	 * @throws PreconditionViolationException if the supplied collection is {@code null} or empty
	 * @see #containsNoNullElements(Collection, String)
	 * @see #condition(boolean, String)
	 */
	@Contract("null, _ -> fail")
	public static <T extends Collection<?>> T notEmpty(@Nullable T collection, Supplier<String> messageSupplier)
			throws PreconditionViolationException {

		condition(collection != null && !collection.isEmpty(), messageSupplier);
		return collection;
	}

	/**
	 * Assert that the supplied array contains no {@code null} elements.
	 *
	 * <p><strong>WARNING</strong>: this method does NOT check if the supplied
	 * array is {@code null} or <em>empty</em>.
	 *
	 * @param array the array to check
	 * @param message precondition violation message
	 * @return the supplied array as a convenience
	 * @throws PreconditionViolationException if the supplied array contains
	 * any {@code null} elements
	 * @see #notNull(Object, String)
	 */

	@Contract("null, _ -> null")
	public static <T> T @Nullable [] containsNoNullElements(T @Nullable [] array, String message)
			throws PreconditionViolationException {

		if (array != null) {
			Arrays.stream(array).forEach(object -> notNull(object, message));
		}
		return array;
	}

	/**
	 * Assert that the supplied array contains no {@code null} elements.
	 *
	 * <p><strong>WARNING</strong>: this method does NOT check if the supplied
	 * array is {@code null} or <em>empty</em>.
	 *
	 * @param array the array to check
	 * @param messageSupplier precondition violation message supplier
	 * @return the supplied array as a convenience
	 * @throws PreconditionViolationException if the supplied array contains
	 * any {@code null} elements
	 * @see #notNull(Object, String)
	 */
	@Contract("null, _ -> null")
	public static <T> T @Nullable [] containsNoNullElements(T @Nullable [] array, Supplier<String> messageSupplier)
			throws PreconditionViolationException {

		if (array != null) {
			Arrays.stream(array).forEach(object -> notNull(object, messageSupplier));
		}
		return array;
	}

	/**
	 * Assert that the supplied collection contains no {@code null} elements.
	 *
	 * <p><strong>WARNING</strong>: this method does NOT check if the supplied
	 * collection is {@code null} or <em>empty</em>.
	 *
	 * @param collection the collection to check
	 * @param message precondition violation message
	 * @return the supplied collection as a convenience
	 * @throws PreconditionViolationException if the supplied collection contains
	 * any {@code null} elements
	 * @see #notNull(Object, String)
	 */
	@Contract("null, _ -> null")
	public static <T extends Collection<?>> @Nullable T containsNoNullElements(@Nullable T collection, String message)
			throws PreconditionViolationException {

		if (collection != null) {
			collection.forEach(object -> notNull(object, message));
		}
		return collection;
	}

	/**
	 * Assert that the supplied collection contains no {@code null} elements.
	 *
	 * <p><strong>WARNING</strong>: this method does NOT check if the supplied
	 * collection is {@code null} or <em>empty</em>.
	 *
	 * @param collection the collection to check
	 * @param messageSupplier precondition violation message supplier
	 * @return the supplied collection as a convenience
	 * @throws PreconditionViolationException if the supplied collection contains
	 * any {@code null} elements
	 * @see #notNull(Object, String)
	 */
	@Contract("null, _ -> null")
	public static <T extends Collection<?>> @Nullable T containsNoNullElements(@Nullable T collection,
			Supplier<String> messageSupplier) throws PreconditionViolationException {

		if (collection != null) {
			collection.forEach(object -> notNull(object, messageSupplier));
		}
		return collection;
	}

	/**
	 * Assert that the supplied {@link String} is not blank.
	 *
	 * <p>A {@code String} is <em>blank</em> if it is {@code null} or consists
	 * only of whitespace characters.
	 *
	 * @param str the string to check
	 * @param message precondition violation message
	 * @return the supplied string as a convenience
	 * @throws PreconditionViolationException if the supplied string is blank
	 * @see #notBlank(String, Supplier)
	 */
	@Contract("null, _ -> fail")
	public static String notBlank(@Nullable String str, String message) throws PreconditionViolationException {
		condition(StringUtils.isNotBlank(str), message);
		return str;
	}

	/**
	 * Assert that the supplied {@link String} is not blank.
	 *
	 * <p>A {@code String} is <em>blank</em> if it is {@code null} or consists
	 * only of whitespace characters.
	 *
	 * @param str the string to check
	 * @param messageSupplier precondition violation message supplier
	 * @return the supplied string as a convenience
	 * @throws PreconditionViolationException if the supplied string is blank
	 * @see StringUtils#isNotBlank(String)
	 * @see #condition(boolean, Supplier)
	 */
	@Contract("null, _ -> fail")
	public static String notBlank(@Nullable String str, Supplier<String> messageSupplier)
			throws PreconditionViolationException {

		condition(StringUtils.isNotBlank(str), messageSupplier);
		return str;
	}

	/**
	 * Assert that the supplied {@code predicate} is {@code true}.
	 *
	 * @param predicate the predicate to check
	 * @param message precondition violation message
	 * @throws PreconditionViolationException if the predicate is {@code false}
	 * @see #condition(boolean, Supplier)
	 */
	@Contract("false, _ -> fail")
	public static void condition(boolean predicate, String message) throws PreconditionViolationException {
		if (!predicate) {
			throw new PreconditionViolationException(message);
		}
	}

	/**
	 * Assert that the supplied {@code predicate} is {@code true}.
	 *
	 * @param predicate the predicate to check
	 * @param messageSupplier precondition violation message supplier
	 * @throws PreconditionViolationException if the predicate is {@code false}
	 */
	@Contract("false, _ -> fail")
	public static void condition(boolean predicate, Supplier<String> messageSupplier)
			throws PreconditionViolationException {

		if (!predicate) {
			throw new PreconditionViolationException(messageSupplier.get());
		}
	}

}
