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

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.ReflectionUtils.invokeMethod;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Collection of utilities for working with {@link Collection Collections}.
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
public final class CollectionUtils {

	private CollectionUtils() {
		/* no-op */
	}

	/**
	 * Get the only element of a collection of size 1.
	 *
	 * @param collection the collection to get the element from
	 * @return the only element of the collection
	 * @throws PreconditionViolationException if the collection is {@code null}
	 * or does not contain exactly one element
	 */
	public static <T> T getOnlyElement(Collection<T> collection) {
		Preconditions.notNull(collection, "collection must not be null");
		Preconditions.condition(collection.size() == 1,
			() -> "collection must contain exactly one element: " + collection);
		return firstElement(collection);
	}

	/**
	 * Get the first element of the supplied collection unless it's empty.
	 *
	 * @param collection the collection to get the element from
	 * @return the first element of the collection; empty if the collection is empty
	 * @throws PreconditionViolationException if the collection is {@code null}
	 * @since 1.11
	 */
	@API(status = INTERNAL, since = "1.11")
	public static <T extends @Nullable Object> Optional<T> getFirstElement(Collection<T> collection) {
		Preconditions.notNull(collection, "collection must not be null");
		return collection.isEmpty() //
				? Optional.empty() //
				: Optional.ofNullable(firstElement(collection));
	}

	private static <T extends @Nullable Object> T firstElement(Collection<T> collection) {
		return collection instanceof List<T> list //
				? list.get(0) //
				: collection.iterator().next();
	}

	/**
	 * Determine if an instance of the supplied type can be converted into a
	 * {@code Stream}.
	 *
	 * <p>If this method returns {@code true}, {@link #toStream(Object)} can
	 * successfully convert an object of the specified type into a stream. See
	 * {@link #toStream(Object)} for supported types.
	 *
	 * @param type the type to check; may be {@code null}
	 * @return {@code true} if an instance of the type can be converted into a stream
	 * @since 1.9.1
	 * @see #toStream(Object)
	 */
	@API(status = INTERNAL, since = "1.9.1")
	public static boolean isConvertibleToStream(@Nullable Class<?> type) {
		if (type == null || type == void.class) {
			return false;
		}
		return (Stream.class.isAssignableFrom(type)//
				|| DoubleStream.class.isAssignableFrom(type)//
				|| IntStream.class.isAssignableFrom(type)//
				|| LongStream.class.isAssignableFrom(type)//
				|| Iterable.class.isAssignableFrom(type)//
				|| Iterator.class.isAssignableFrom(type)//
				|| Object[].class.isAssignableFrom(type)//
				|| (type.isArray() && type.getComponentType().isPrimitive())//
				|| findIteratorMethod(type).isPresent());
	}

	/**
	 * Convert an object of one of the following supported types into a {@code Stream}.
	 *
	 * <ul>
	 * <li>{@link Stream}</li>
	 * <li>{@link DoubleStream}</li>
	 * <li>{@link IntStream}</li>
	 * <li>{@link LongStream}</li>
	 * <li>{@link Collection}</li>
	 * <li>{@link Iterable}</li>
	 * <li>{@link Iterator}</li>
	 * <li>{@link Object} array</li>
	 * <li>primitive array</li>
	 * <li>any type that provides an
	 * {@link java.util.Iterator Iterator}-returning {@code iterator()} method
	 * (such as, for example, a {@code kotlin.sequences.Sequence})</li>
	 * </ul>
	 *
	 * @param object the object to convert into a stream; never {@code null}
	 * @return the resulting stream
	 * @throws PreconditionViolationException if the supplied object is {@code null}
	 * or not one of the supported types
	 * @see #isConvertibleToStream(Class)
	 */
	public static Stream<?> toStream(Object object) {
		Preconditions.notNull(object, "Object must not be null");
		if (object instanceof Stream<?> stream) {
			return stream;
		}
		if (object instanceof DoubleStream stream) {
			return stream.boxed();
		}
		if (object instanceof IntStream stream) {
			return stream.boxed();
		}
		if (object instanceof LongStream stream) {
			return stream.boxed();
		}
		if (object instanceof Collection<?> collection) {
			return collection.stream();
		}
		if (object instanceof Iterable<?> iterable) {
			return stream(iterable.spliterator(), false);
		}
		if (object instanceof Iterator<?> iterator) {
			return stream(spliteratorUnknownSize(iterator, ORDERED), false);
		}
		if (object instanceof Object[] array) {
			return Arrays.stream(array);
		}
		if (object instanceof double[] array) {
			return DoubleStream.of(array).boxed();
		}
		if (object instanceof int[] array) {
			return IntStream.of(array).boxed();
		}
		if (object instanceof long[] array) {
			return LongStream.of(array).boxed();
		}
		if (object.getClass().isArray() && object.getClass().getComponentType().isPrimitive()) {
			return IntStream.range(0, Array.getLength(object)).mapToObj(i -> Array.get(object, i));
		}
		return tryConvertToStreamByReflection(object);
	}

	private static Stream<?> tryConvertToStreamByReflection(Object object) {
		return findIteratorMethod(object.getClass()) //
				.map(method -> (Iterator<?>) invokeMethod(method, object)) //
				.map(iterator -> spliteratorUnknownSize(iterator, ORDERED)) //
				.map(spliterator -> stream(spliterator, false)) //
				.orElseThrow(() -> new PreconditionViolationException(
					"Cannot convert instance of %s into a Stream: %s".formatted(object.getClass().getName(), object)));
	}

	private static Optional<Method> findIteratorMethod(Class<?> type) {
		return ReflectionUtils.findMethod(type, "iterator") //
				.filter(method -> method.getReturnType() == Iterator.class);
	}

	/**
	 * Call the supplied action on each element of the supplied {@link List} from last to first element.
	 */
	@API(status = INTERNAL, since = "1.9.2")
	public static <T extends @Nullable Object> void forEachInReverseOrder(List<T> list, Consumer<? super T> action) {
		if (list.isEmpty()) {
			return;
		}
		if (list.size() == 1) {
			action.accept(list.get(0));
			return;
		}
		for (ListIterator<T> iterator = list.listIterator(list.size()); iterator.hasPrevious();) {
			action.accept(iterator.previous());
		}
	}

}
