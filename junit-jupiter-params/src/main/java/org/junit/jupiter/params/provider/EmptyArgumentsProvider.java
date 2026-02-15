/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.platform.commons.util.ReflectionUtils.newInstance;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.support.ParameterDeclaration;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.util.Preconditions;

/**
 * @since 5.4
 * @see EmptySource
 */
class EmptyArgumentsProvider implements ArgumentsProvider {

	@Override
	public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {

		Optional<ParameterDeclaration> firstParameter = parameters.getFirst();

		Preconditions.condition(firstParameter.isPresent(),
			() -> "@EmptySource cannot provide an empty argument to %s: no formal parameters declared.".formatted(
				parameters.getSourceElementDescription()));

		Class<?> parameterType = firstParameter.get().getParameterType();

		if (String.class.equals(parameterType)) {
			return Stream.of(arguments(""));
		}
		if (Iterable.class.equals(parameterType)) {
			return Stream.of(arguments(EmptyIterable.INSTANCE));
		}
		if (Iterator.class.equals(parameterType)) {
			return Stream.of(arguments(EmptyIterator.INSTANCE));
		}
		if (ListIterator.class.equals(parameterType)) {
			return Stream.of(arguments(EmptyListIterator.INSTANCE));
		}
		if (Collection.class.equals(parameterType)) {
			return Stream.of(arguments(Collections.emptySet()));
		}
		if (List.class.equals(parameterType)) {
			return Stream.of(arguments(Collections.emptyList()));
		}
		if (Set.class.equals(parameterType)) {
			return Stream.of(arguments(Collections.emptySet()));
		}
		if (SortedSet.class.equals(parameterType)) {
			return Stream.of(arguments(Collections.emptySortedSet()));
		}
		if (NavigableSet.class.equals(parameterType)) {
			return Stream.of(arguments(Collections.emptyNavigableSet()));
		}
		if (Map.class.equals(parameterType)) {
			return Stream.of(arguments(Collections.emptyMap()));
		}
		if (SortedMap.class.equals(parameterType)) {
			return Stream.of(arguments(Collections.emptySortedMap()));
		}
		if (NavigableMap.class.equals(parameterType)) {
			return Stream.of(arguments(Collections.emptyNavigableMap()));
		}
		if (Collection.class.isAssignableFrom(parameterType) || Map.class.isAssignableFrom(parameterType)) {
			Optional<Constructor<?>> defaultConstructor = getDefaultConstructor(parameterType);
			if (defaultConstructor.isPresent()) {
				return Stream.of(arguments(newInstance(defaultConstructor.get())));
			}
		}
		if (parameterType.isArray()) {
			Object array = Array.newInstance(parameterType.getComponentType(), 0);
			return Stream.of(arguments(array));
		}
		// else
		throw new PreconditionViolationException(
			"@EmptySource cannot provide an empty argument to %s: [%s] is not a supported type.".formatted(
				parameters.getSourceElementDescription(), parameterType.getName()));
	}

	private static Optional<Constructor<?>> getDefaultConstructor(Class<?> clazz) {
		try {
			return Optional.of(clazz.getConstructor());
		}
		catch (NoSuchMethodException e) {
			return Optional.empty();
		}
	}

	/**
	 * @since 6.1
	 */
	private static class EmptyIterable<E> implements Iterable<E> {

		private static final EmptyIterable<Object> INSTANCE = new EmptyIterable<>();

		@Override
		@SuppressWarnings("unchecked")
		public Iterator<E> iterator() {
			return (Iterator<E>) EmptyIterator.INSTANCE;
		}

		@Override
		public String toString() {
			return "[]";
		}
	}

	/**
	 * @since 6.1
	 */
	private static sealed class EmptyIterator<E> implements Iterator<E> permits EmptyListIterator {

		private static final EmptyIterator<Object> INSTANCE = new EmptyIterator<>();

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public E next() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return "[]";
		}
	}

	/**
	 * @since 6.1
	 */
	private static final class EmptyListIterator<E> extends EmptyIterator<E> implements ListIterator<E> {

		private static final EmptyListIterator<Object> INSTANCE = new EmptyListIterator<>();

		@Override
		public boolean hasPrevious() {
			return false;
		}

		@Override
		public E previous() {
			throw new NoSuchElementException();
		}

		@Override
		public int nextIndex() {
			return 0;
		}

		@Override
		public int previousIndex() {
			return -1;
		}

		@Override
		public void set(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(E e) {
			throw new UnsupportedOperationException();
		}
	}

}
