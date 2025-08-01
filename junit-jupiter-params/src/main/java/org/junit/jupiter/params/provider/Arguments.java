/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;

/**
 * {@code Arguments} is an abstraction that provides access to an array of
 * objects to be used for invoking a {@code @ParameterizedTest} method.
 *
 * <p>A {@link java.util.stream.Stream} of such {@code Arguments} will
 * typically be provided by an {@link ArgumentsProvider}.
 *
 * @apiNote <p>This interface is specifically designed as a simple holder of
 * arguments for a parameterized test. Therefore, if you end up
 * {@linkplain java.util.stream.Stream#map(java.util.function.Function) transforming} or
 * {@linkplain java.util.stream.Stream#filter(java.util.function.Predicate) filtering}
 * the arguments, you should consider using one of the following in intermediate
 * steps:
 *
 * <ul>
 *   <li>The standard Java collections</li>
 *   <li>Tuples from third-party libraries &mdash; for example,
 *   <a href="https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/tuple/package-summary.html">Commons Lang</a>
 *   or <a href="https://www.javatuples.org">javatuples</a></li>
 *   <li>Your own data class</li>
 * </ul>
 *
 * <p>Alternatively, you can use an
 * {@link org.junit.jupiter.params.converter.ArgumentConverter ArgumentConverter}
 * to convert some of the arguments from one type to another.
 *
 * @since 5.0
 * @see ArgumentSet
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.provider.ArgumentsProvider
 * @see org.junit.jupiter.params.converter.ArgumentConverter
 */
@FunctionalInterface
@API(status = STABLE, since = "5.7")
public interface Arguments {

	/**
	 * Get the arguments used for an invocation of the
	 * {@code @ParameterizedTest} method.
	 *
	 * @apiNote If you need a type-safe way to access some or all of the arguments,
	 * please read the {@linkplain Arguments class-level API note}.
	 *
	 * @return the arguments; never {@code null} but may contain {@code null}
	 */
	@Nullable
	Object[] get();

	/**
	 * Factory method for creating an instance of {@code Arguments} based on
	 * the supplied {@code arguments}.
	 *
	 * @param arguments the arguments to be used for an invocation of the test
	 * method; must not be {@code null} but may contain {@code null}
	 * @return an instance of {@code Arguments}; never {@code null}
	 * @see #arguments(Object...)
	 * @see #argumentSet(String, Object...)
	 */
	static Arguments of(@Nullable Object... arguments) {
		Preconditions.notNull(arguments, "arguments array must not be null");
		return () -> arguments;
	}

	/**
	 * Factory method for creating an instance of {@code Arguments} based on
	 * the supplied {@code arguments}.
	 *
	 * <p>This method is an <em>alias</em> for {@link Arguments#of} and is
	 * intended to be used when statically imported &mdash; for example, via:
	 * {@code import static org.junit.jupiter.params.provider.Arguments.arguments;}
	 *
	 * @param arguments the arguments to be used for an invocation of the test
	 * method; must not be {@code null} but may contain {@code null}
	 * @return an instance of {@code Arguments}; never {@code null}
	 * @since 5.3
	 * @see #argumentSet(String, Object...)
	 */
	static Arguments arguments(@Nullable Object... arguments) {
		return of(arguments);
	}

	/**
	 * Factory method for creating an {@link ArgumentSet} based on the supplied
	 * {@code name} and {@code arguments}.
	 *
	 * <p>Favor this method over {@link Arguments#of Arguments.of(...)} and
	 * {@link Arguments#arguments arguments(...)} when you wish to assign a name
	 * to the entire set of arguments.
	 *
	 * <p>This method is well suited to be used as a static import &mdash; for
	 * example, via:
	 * {@code import static org.junit.jupiter.params.provider.Arguments.argumentSet;}.
	 *
	 * @param name the name of the argument set; must not be {@code null} or blank
	 * @param arguments the arguments to be used for an invocation of the test
	 * method; must not be {@code null} but may contain {@code null}
	 * @return an {@code ArgumentSet}; never {@code null}
	 * @since 5.11
	 * @see ArgumentSet
	 * @see org.junit.jupiter.params.ParameterizedTest#ARGUMENT_SET_NAME_PLACEHOLDER
	 * @see org.junit.jupiter.params.ParameterizedTest#ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	static ArgumentSet argumentSet(String name, @Nullable Object... arguments) {
		return new ArgumentSet(name, arguments);
	}

	/**
	 * Factory method for creating an instance of {@code Arguments} based on
	 * the supplied {@link List} of {@code arguments}.
	 *
	 * @param arguments the arguments to be used for an invocation of the test
	 * method; must not be {@code null} but may contain {@code null}.
	 * @return an instance of {@code Arguments}; never {@code null}.
	 * @since 6.0
	 * @see #argumentsFrom(List)
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	static Arguments from(List<@Nullable Object> arguments) {
		Preconditions.notNull(arguments, "arguments must not be null");
		return of(arguments.toArray());
	}

	/**
	 * Factory method for creating an instance of {@code Arguments} based on
	 * the supplied {@link List} of {@code arguments}.
	 *
	 * <p>This method is an <em>alias</em> for {@link Arguments#from} and is
	 * intended to be used when statically imported &mdash; for example, via:
	 * {@code import static org.junit.jupiter.params.provider.Arguments.argumentsFrom;}
	 *
	 * @param arguments the arguments to be used for an invocation of the test
	 * method; must not be {@code null} but may contain {@code null}.
	 * @return an instance of {@code Arguments}; never {@code null}.
	 * @since 6.0
	 * @see #argumentSet(String, Object...)
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	static Arguments argumentsFrom(List<@Nullable Object> arguments) {
		return from(arguments);
	}

	/**
	 * Factory method for creating an {@link ArgumentSet} based on the supplied
	 * {@code name} and {@link List} of {@code arguments}.
	 *
	 * <p>This method is a convenient alternative to
	 * {@link #argumentSet(String, Object...)} when working with {@link List}
	 * based inputs.
	 *
	 * @param name the name of the argument set; must not be {@code null}
	 * or blank.
	 * @param arguments the arguments to be used for an invocation of the test
	 * method; must not be {@code null} but may contain {@code null}.
	 * @return an {@code ArgumentSet}; never {@code null}.
	 * @since 6.0
	 * @see #argumentSet(String, Object...)
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	static ArgumentSet argumentSetFrom(String name, List<@Nullable Object> arguments) {
		Preconditions.notBlank(name, "name must not be null or blank");
		Preconditions.notNull(arguments, "arguments list must not be null");
		return new ArgumentSet(name, arguments.toArray());
	}

	/**
	 * Convert the arguments to a new mutable {@link List} containing the same
	 * elements as {@link #get()}.
	 *
	 * <p>This is useful for test logic that benefits from {@code List}
	 * operations such as filtering, transformation, or assertions.
	 *
	 * @return a mutable List of arguments; never {@code null} but may contain
	 * {@code null}.
	 * @since 6.0
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	default List<@Nullable Object> toList() {
		return new ArrayList<>(Arrays.asList(get()));
	}

	/**
	 * Specialization of {@link Arguments} that associates a {@link #getName() name}
	 * with a set of {@link #get() arguments}.
	 *
	 * @since 5.11
	 * @see Arguments#argumentSet(String, Object...)
	 * @see org.junit.jupiter.params.ParameterizedTest#ARGUMENT_SET_NAME_PLACEHOLDER
	 * @see org.junit.jupiter.params.ParameterizedTest#ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	final class ArgumentSet implements Arguments {

		private final String name;

		private final @Nullable Object[] arguments;

		private ArgumentSet(String name, @Nullable Object[] arguments) {
			Preconditions.notBlank(name, "name must not be null or blank");
			Preconditions.notNull(arguments, "arguments array must not be null");
			this.name = name;
			this.arguments = arguments;
		}

		/**
		 * Get the name of this {@code ArgumentSet}.
		 * @return the name of this {@code ArgumentSet}; never {@code null} or blank
		 */
		public String getName() {
			return this.name;
		}

		@Override
		public @Nullable Object[] get() {
			return this.arguments;
		}

		/**
		 * Return the {@link #getName() name} of this {@code ArgumentSet}.
		 * @return the name of this {@code ArgumentSet}
		 */
		@Override
		public String toString() {
			return getName();
		}

	}
}
