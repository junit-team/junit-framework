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

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @MethodSource} is a {@linkplain Repeatable repeatable}
 * {@link ArgumentsSource} which provides access to values returned from
 * {@linkplain #value() factory methods} of the class in which this annotation
 * is declared or from static factory methods in external classes referenced
 * by <em>fully qualified method name</em>.
 *
 * <p>Each factory method must generate a <em>stream</em> of <em>arguments</em>,
 * and each set of "arguments" within the "stream" will be provided as the
 * physical arguments for individual invocations of the annotated
 * {@code org.junit.jupiter.params.ParameterizedClass @ParameterizedClass} or
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}.
 * Generally speaking this translates to a {@link java.util.stream.Stream Stream}
 * of {@link Arguments} (i.e., {@code Stream<Arguments>}); however, the actual
 * concrete return type can take on many forms. In this context, a "stream" is
 * anything that JUnit can reliably convert into a {@code Stream}, such as
 * {@link java.util.stream.Stream Stream},
 * {@link java.util.stream.DoubleStream DoubleStream},
 * {@link java.util.stream.LongStream LongStream},
 * {@link java.util.stream.IntStream IntStream},
 * {@link java.util.Collection Collection},
 * {@link java.util.Iterator Iterator}, an array of objects or primitives, or
 * any type that provides an {@link java.util.Iterator Iterator}-returning
 * {@code iterator()} method (such as, for example, a
 * {@code kotlin.sequences.Sequence}). Each set of "arguments" within the
 * "stream" can be supplied as an instance of {@link Arguments}, an array of
 * objects (e.g., {@code Object[]}, {@code String[]}, etc.), or a single
 * <em>value</em> if the parameterized test method accepts a single argument.
 *
 * <p>If the return type is {@code Stream} or
 * one of the primitive streams, JUnit will properly close it by calling
 * {@link java.util.stream.BaseStream#close() BaseStream.close()},
 * making it safe to use a resource such as
 * {@link java.nio.file.Files#lines(java.nio.file.Path) Files.lines()}.
 *
 * <p>Please note that a one-dimensional array of objects supplied as a set of
 * "arguments" will be handled differently than other types of arguments.
 * Specifically, all of the elements of a one-dimensional array of objects will
 * be passed as individual physical arguments to the {@code @ParameterizedTest}
 * method. This behavior can be seen in the table below for the
 * {@code static Stream<Object[]> factory()} method: the {@code @ParameterizedTest}
 * method accepts individual {@code String} and {@code int} arguments rather than
 * a single {@code Object[]} array. In contrast, any multidimensional array
 * supplied as a set of "arguments" will be passed as a single physical argument
 * to the {@code @ParameterizedTest} method without modification. This behavior
 * can be seen in the table below for the {@code static Stream<int[][]> factory()}
 * and {@code static Stream<Object[][]> factory()} methods: the
 * {@code @ParameterizedTest} methods for those factories accept individual
 * {@code int[][]} and {@code Object[][]} arguments, respectively.
 *
 * <h2>Examples</h2>
 *
 * <p>The following table displays compatible method signatures for parameterized
 * test methods and their corresponding factory methods.
 *
 * <table class="plain">
 * <caption>Compatible method signatures and factory methods</caption>
 * <tr><th>{@code @ParameterizedTest} method</th><th>Factory method</th></tr>
 * <tr><td>{@code void test(int)}</td><td>{@code static int[] factory()}</td></tr>
 * <tr><td>{@code void test(int)}</td><td>{@code static IntStream factory()}</td></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static String[] factory()}</td></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static List<String> factory()}</td></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static Stream<String> factory()}</td></tr>
 * <tr><td>{@code void test(String, String)}</td><td>{@code static String[][] factory()}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Object[][] factory()}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Stream<Object[]> factory()}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Stream<Arguments> factory()}</td></tr>
 * <tr><td>{@code void test(int[])}</td><td>{@code static int[][] factory()}</td></tr>
 * <tr><td>{@code void test(int[])}</td><td>{@code static Stream<int[]> factory()}</td></tr>
 * <tr><td>{@code void test(int[][])}</td><td>{@code static Stream<int[][]> factory()}</td></tr>
 * <tr><td>{@code void test(Object[][])}</td><td>{@code static Stream<Object[][]> factory()}</td></tr>
 * </table>
 *
 * <p>Factory methods within the test class must be {@code static} unless the
 * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS PER_CLASS}
 * test instance lifecycle mode is used; whereas, factory methods in external
 * classes must always be {@code static}.
 *
 * <p>This behavior and the above examples also apply to parameters of a
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass},
 * regardless whether field or constructor injection is used.
 *
 * <p>Factory methods can declare parameters, which will be provided by registered
 * implementations of {@link org.junit.jupiter.api.extension.ParameterResolver}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.0
 * @see FieldSource
 * @see Arguments
 * @see ArgumentsSource
 * @see org.junit.jupiter.params.ParameterizedClass
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see org.junit.jupiter.api.TestInstance
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(MethodSources.class)
@API(status = STABLE, since = "5.7")
@ArgumentsSource(MethodArgumentsProvider.class)
@SuppressWarnings("exports")
public @interface MethodSource {

	/**
	 * The names of factory methods within the test class or in external classes
	 * to use as sources for arguments.
	 *
	 * <p>Factory methods in external classes must be referenced by
	 * <em>fully qualified method name</em> &mdash; for example,
	 * {@code "com.example.StringsProviders#blankStrings"} or
	 * {@code "com.example.TopLevelClass$NestedClass#classMethod"} for a factory
	 * method in a static nested class.
	 *
	 * <p>If a factory method accepts arguments that are provided by a
	 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolver},
	 * you can supply the formal parameter list in the qualified method name to
	 * disambiguate between overloaded variants of the factory method. For example,
	 * {@code "blankStrings(int)"} for a local qualified method name or
	 * {@code "com.example.StringsProviders#blankStrings(int)"} for a fully qualified
	 * method name.
	 *
	 * <p>If no factory method names are declared, a method within the test class
	 * that has the same name as the test method will be used as the factory
	 * method by default in case this annotation is applied to a
	 * {@code @ParameterizedTest} method. For a {@code @ParameterizedClass}, at
	 * least one method name must be declared explicitly.
	 *
	 * <p>For further information, see the {@linkplain MethodSource class-level Javadoc}.
	 */
	String[] value() default "";

}
