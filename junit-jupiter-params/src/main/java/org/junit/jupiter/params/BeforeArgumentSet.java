/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

/**
 * {@code @BeforeArgumentSet} is used to signal that the annotated method should
 * be executed <em>before</em> <strong>each</strong> set of arguments of the
 * current {@link ParameterizedClass @ParameterizedClass}.
 *
 * <p>Declaring {@code @BeforeArgumentSet} methods in a regular,
 * non-parameterized test class has no effect and will be ignored.
 *
 * <h2>Method Signatures</h2>
 *
 * <p>{@code @BeforeArgumentSet} methods must have a {@code void} return type,
 * must not be private, and must be {@code static} by default. Consequently,
 * {@code @BeforeArgumentSet} methods are not supported in
 * {@link org.junit.jupiter.api.Nested @Nested} test classes or as <em>interface
 * default methods</em> unless the test class is annotated with
 * {@link org.junit.jupiter.api.TestInstance @TestInstance(Lifecycle.PER_CLASS)}.
 * However, beginning with Java 16 {@code @BeforeArgumentSet} methods may be
 * declared as {@code static} in {@link org.junit.jupiter.api.Nested @Nested}
 * test classes, in which case the {@code Lifecycle.PER_CLASS} restriction no
 * longer applies.
 *
 * <h2>Method Arguments</h2>
 *
 * <p>{@code @BeforeArgumentSet} methods may optionally declare parameters that
 * are resolved depending on the setting of the {@link #injectArguments()}
 * attribute.
 *
 * <p>If {@link #injectArguments()} is set to {@code false} (the default), the
 * parameters must be resolved by other registered
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolvers}.
 *
 * <p>If {@link #injectArguments()} is set to {@code true}, the method must
 * declare the same parameters, in the same order, as the <em>indexed
 * parameters</em> (see {@link ParameterizedClass @ParameterizedClass}) of the
 * parameterized test class. It may declare a subset of the indexed parameters
 * starting from the first argument. Additionally, the method may declare custom
 * <em>aggregator parameters</em> (see
 * {@link ParameterizedClass @ParameterizedClass}) at the end of its parameter
 * list. If the method declares additional parameters after these aggregator
 * parameters, or more parameters than the class has indexed
 * parameters, they may be resolved by other
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolvers}.
 *
 * <p>For example, given a {@link ParameterizedClass @ParameterizedClass} with
 * <em>indexed parameters</em> of type {@code int} and {@code String}, the
 * following method signatures are valid:
 *
 * <pre>{@code
 * @BeforeArgumentSet(injectArguments = true)
 * void beforeArgumentSet() { ... }
 *
 * @BeforeArgumentSet(injectArguments = true)
 * void beforeArgumentSet(int number) { ... }
 *
 * @BeforeArgumentSet(injectArguments = true)
 * void beforeArgumentSet(int number, String text) { ... }
 *
 * @BeforeArgumentSet(injectArguments = true)
 * void beforeArgumentSet(int number, String text, TestInfo testInfo) { ... }
 *
 * @BeforeArgumentSet(injectArguments = true)
 * void beforeArgumentSet(ArgumentsAccessor accessor) { ... }
 *
 * @BeforeArgumentSet(injectArguments = true)
 * void beforeArgumentSet(ArgumentsAccessor accessor, TestInfo testInfo) { ... }
 *
 * @BeforeArgumentSet(injectArguments = true)
 * void beforeArgumentSet(int number, String text, ArgumentsAccessor accessor) { ... }
 *
 * @BeforeArgumentSet(injectArguments = true)
 * void beforeArgumentSet(int number, String text, ArgumentsAccessor accessor, TestInfo testInfo) { ... }
 * }</pre>
 *
 * <p>In the snippet above,{@link ArgumentsAccessor} is used as an example of an
 * aggregator parameter but the same applies to any parameter annotated with
 * {@link AggregateWith @AggregateWith}. The parameter of type
 * {@link org.junit.jupiter.api.TestInfo TestInfo} is used as an example of a
 * parameter that is resolved by another
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolver}.
 *
 * <h2>Inheritance and Execution Order</h2>
 *
 * <p>{@code @BeforeArgumentSet} methods are inherited from superclasses as long
 * as they are not <em>overridden</em> according to the visibility rules of the
 * Java language. Furthermore, {@code @BeforeArgumentSet} methods from
 * superclasses will be executed before {@code @BeforeArgumentSet} methods in
 * subclasses.
 *
 * <p>Similarly, {@code @BeforeArgumentSet} methods declared in an interface are
 * inherited as long as they are not overridden, and {@code @BeforeArgumentSet}
 * methods from an interface will be executed before {@code @BeforeArgumentSet}
 * methods in the class that implements the interface.
 *
 * <p>JUnit Jupiter does not guarantee the execution order of multiple
 * {@code @BeforeArgumentSet} methods that are declared within a single
 * parameterized test class or test interface. While it may at times appear that
 * these methods are invoked in alphabetical order, they are in fact sorted
 * using an algorithm that is deterministic but intentionally non-obvious.
 *
 * <p>In addition, {@code @BeforeArgumentSet} methods are in no way linked to
 * {@code @AfterArgumentSet} methods. Consequently, there are no guarantees with
 * regard to their <em>wrapping</em> behavior. For example, given two
 * {@code @BeforeArgumentSet} methods {@code createA()} and {@code createB()} as
 * well as two {@code @AfterArgumentSet} methods {@code destroyA()} and
 * {@code destroyB()}, the order in which the {@code @BeforeArgumentSet} methods
 * are executed (e.g. {@code createA()} before {@code createB()}) does not imply
 * any order for the seemingly corresponding {@code @AfterArgumentSet} methods.
 * In other words, {@code destroyA()} might be called before <em>or</em> after
 * {@code destroyB()}. The JUnit Team therefore recommends that developers
 * declare at most one {@code @BeforeArgumentSet} method and at most one
 * {@code @AfterArgumentSet} method per test class or test interface unless
 * there are no dependencies between the {@code @BeforeArgumentSet} methods or
 * between the {@code @AfterArgumentSet} methods.
 *
 * <h2>Composition</h2>
 *
 * <p>{@code @BeforeArgumentSet} may be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics of
 * {@code @BeforeArgumentSet}.
 *
 * @since 5.13
 * @see ParameterizedClass
 * @see AfterArgumentSet
 * @see org.junit.jupiter.api.TestInstance
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = EXPERIMENTAL, since = "5.13")
public @interface BeforeArgumentSet {

	/**
	 * Whether the arguments of the parameterized test class should be injected
	 * into the annotated method (defaults to {@code false}).
	 */
	boolean injectArguments() default false;

}
