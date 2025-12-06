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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @FieldSource} is a {@linkplain Repeatable repeatable}
 * {@link ArgumentsSource} which provides access to values of
 * {@linkplain #value() fields} of the class in which this annotation is declared
 * or from static fields in external classes referenced by <em>fully qualified
 * field name</em>.
 *
 * <p>Each field must be able to supply a <em>stream</em> of <em>arguments</em>,
 * and each set of "arguments" within the "stream" will be provided as the physical
 * arguments for individual invocations of the annotated
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass} or
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}.
 *
 * <p>In this context, a "stream" is anything that JUnit can reliably convert to
 * a {@link java.util.stream.Stream Stream}; however, the actual concrete field
 * type can take on many forms. Generally speaking this translates to a
 * {@link java.util.Collection Collection}, an {@link Iterable}, a
 * {@link java.util.function.Supplier Supplier} of a stream
 * ({@link java.util.stream.Stream Stream},
 * {@link java.util.stream.DoubleStream DoubleStream},
 * {@link java.util.stream.LongStream LongStream}, or
 * {@link java.util.stream.IntStream IntStream}), a {@code Supplier} of an
 * {@link java.util.Iterator Iterator}, an array of objects or primitives, or
 * any type that provides an {@link java.util.Iterator Iterator}-returning
 * {@code iterator()} method (such as, for example, a
 * {@code kotlin.sequences.Sequence}). Each set of "arguments" within the
 * "stream" can be supplied as an instance of {@link Arguments}, an array of
 * objects (for example, {@code Object[]}, {@code String[]}, etc.), or a single
 * <em>value</em> if the parameterized class or test accepts a single argument.
 *
 * <p>In contrast to the supported return types for {@link MethodSource @MethodSource}
 * factory methods, the value of a {@code @FieldSource} field cannot be an instance of
 * {@link java.util.stream.Stream Stream},
 * {@link java.util.stream.DoubleStream DoubleStream},
 * {@link java.util.stream.LongStream LongStream},
 * {@link java.util.stream.IntStream IntStream}, or
 * {@link java.util.Iterator Iterator}, since the values of such types are
 * <em>consumed</em> the first time they are processed. However, if you wish to
 * use one of these types, you can wrap it in a {@code Supplier} &mdash; for
 * example, {@code Supplier<IntStream>}.
 *
 * <p>If the {@code Supplier} return type is {@code Stream} or
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
 * {@code Supplier<Stream<Object[]>> objectArrayStreamSupplier} field: the
 * {@code @ParameterizedTest} method accepts individual {@code String} and
 * {@code int} arguments rather than a single {@code Object[]} array. In contrast,
 * any multidimensional array supplied as a set of "arguments" will be passed as
 * a single physical argument to the {@code @ParameterizedTest} method without
 * modification. This behavior can be seen in the table below for the
 * {@code Supplier<Stream<int[][]>> twoDimensionalIntArrayStreamSupplier} and
 * {@code Supplier<Stream<Object[][]>> twoDimensionalObjectArrayStreamSupplier}
 * fields: the {@code @ParameterizedTest} methods for those fields accept individual
 * {@code int[][]} and {@code Object[][]} arguments, respectively.
 *
 * <h2>Examples</h2>
 *
 * <p>The following table displays compatible method signatures for parameterized
 * test methods and their corresponding {@code @FieldSource} fields.
 *
 * <table class="plain">
 * <caption>Compatible method signatures and field declarations</caption>
 * <tr><th>{@code @ParameterizedTest} method</th><th>{@code @FieldSource} field</th></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static List<String> listOfStrings}</td></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static String[] arrayOfStrings}</td></tr>
 * <tr><td>{@code void test(int)}</td><td>{@code static int[] intArray}</td></tr>
 * <tr><td>{@code void test(int[])}</td><td>{@code static int[][] twoDimensionalIntArray}</td></tr>
 * <tr><td>{@code void test(String, String)}</td><td>{@code static String[][] twoDimensionalStringArray}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Object[][] twoDimensionalObjectArray}</td></tr>
 * <tr><td>{@code void test(int)}</td><td>{@code static Supplier<IntStream> intStreamSupplier}</td></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static Supplier<Stream<String>> stringStreamSupplier}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Supplier<Stream<Object[]>> objectArrayStreamSupplier}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Supplier<Stream<Arguments>> argumentsStreamSupplier}</td></tr>
 * <tr><td>{@code void test(int[])}</td><td>{@code static Supplier<Stream<int[]>> intArrayStreamSupplier}</td></tr>
 * <tr><td>{@code void test(int[][])}</td><td>{@code static Supplier<Stream<int[][]>> twoDimensionalIntArrayStreamSupplier}</td></tr>
 * <tr><td>{@code void test(Object[][])}</td><td>{@code static Supplier<Stream<Object[][]>> twoDimensionalObjectArrayStreamSupplier}</td></tr>
 * </table>
 *
 * <p>Fields within the test class must be {@code static} unless the
 * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS PER_CLASS}
 * test instance lifecycle mode is used; whereas, fields in external classes must
 * always be {@code static}.
 *
 * <p>This behavior and the above examples also apply to parameters of a
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass},
 * regardless whether field or constructor injection is used.
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.11
 * @see MethodSource
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
@Repeatable(FieldSources.class)
@API(status = MAINTAINED, since = "5.13.3")
@ArgumentsSource(FieldArgumentsProvider.class)
@SuppressWarnings("exports")
public @interface FieldSource {

	/**
	 * The names of fields within the test class or in external classes to use
	 * as sources for arguments.
	 *
	 * <p>Fields in external classes must be referenced by <em>fully qualified
	 * field name</em> &mdash; for example,
	 * {@code "com.example.WebUtils#httpMethodNames"} or
	 * {@code "com.example.TopLevelClass$NestedClass#numbers"} for a field in a
	 * static nested class.
	 *
	 * <p>If no field names are declared, a field within the test class that has
	 * the same name as the test method will be used as the field by default in
	 * case this annotation is applied to a {@code @ParameterizedTest} method.
	 * For a {@code @ParameterizedClass}, at least one field name must be
	 * declared explicitly.
	 *
	 * <p>For further information, see the {@linkplain FieldSource class-level Javadoc}.
	 */
	String[] value() default {};

}
