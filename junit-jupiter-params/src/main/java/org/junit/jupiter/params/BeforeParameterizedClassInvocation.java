/*
 * Copyright 2015-2026 the original author or authors.
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
import org.junit.jupiter.api.extension.ClassTemplateInvocationLifecycleMethod;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;

/**
 * {@code @BeforeParameterizedClassInvocation} is used to signal that the
 * annotated method should be executed <em>before</em> <strong>each</strong>
 * invocation of the current {@link ParameterizedClass @ParameterizedClass}.
 *
 * <p>Declaring {@code @BeforeParameterizedClassInvocation} methods in a
 * regular, non-parameterized test class has no effect and will be ignored.
 *
 * <h2>Method Signatures</h2>
 *
 * <p>{@code @BeforeParameterizedClassInvocation} methods must have a
 * {@code void} return type, must not be private, and must be {@code static}
 * unless the test class is annotated with
 * {@link org.junit.jupiter.api.TestInstance @TestInstance(Lifecycle.PER_CLASS)}.
 *
 * <h2>Method Arguments</h2>
 *
 * <p>{@code @BeforeParameterizedClassInvocation} methods may optionally declare
 * parameters that are resolved depending on the setting of the
 * {@link #injectArguments()} attribute.
 *
 * <p>If {@link #injectArguments()} is set to {@code false}, the parameters must
 * be resolved by other registered
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolvers}.
 *
 * <p>If {@link #injectArguments()} is set to {@code true} (the default), the
 * method must declare the same parameters, in the same order, as the
 * <em>indexed parameters</em> (see
 * {@link ParameterizedClass @ParameterizedClass}) of the parameterized test
 * class. It may declare a subset of the indexed parameters starting from the
 * first argument. Additionally, the method may declare custom <em>aggregator
 * parameters</em> (see {@link ParameterizedClass @ParameterizedClass}) at the
 * end of its parameter list. If the method declares additional parameters after
 * these aggregator parameters, or more parameters than the class has indexed
 * parameters, they may be resolved by other
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolvers}.
 *
 * <p>For example, given a {@link ParameterizedClass @ParameterizedClass} with
 * <em>indexed parameters</em> of type {@code int} and {@code String}, the
 * following method signatures are valid:
 *
 * <pre>{@code
 * @BeforeParameterizedClassInvocation
 * void beforeInvocation() { ... }
 *
 * @BeforeParameterizedClassInvocation
 * void beforeInvocation(int number) { ... }
 *
 * @BeforeParameterizedClassInvocation
 * void beforeInvocation(int number, String text) { ... }
 *
 * @BeforeParameterizedClassInvocation
 * void beforeInvocation(int number, String text, TestInfo testInfo) { ... }
 *
 * @BeforeParameterizedClassInvocation
 * void beforeInvocation(ArgumentsAccessor accessor) { ... }
 *
 * @BeforeParameterizedClassInvocation
 * void beforeInvocation(ArgumentsAccessor accessor, TestInfo testInfo) { ... }
 *
 * @BeforeParameterizedClassInvocation
 * void beforeInvocation(int number, String text, ArgumentsAccessor accessor) { ... }
 *
 * @BeforeParameterizedClassInvocation
 * void beforeInvocation(int number, String text, ArgumentsAccessor accessor, TestInfo testInfo) { ... }
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
 * <p>{@code @BeforeParameterizedClassInvocation} methods are inherited from
 * superclasses as long as they are not <em>overridden</em> according to the
 * visibility rules of the Java language. Furthermore,
 * {@code @BeforeParameterizedClassInvocation} methods from superclasses will be
 * executed before {@code @BeforeParameterizedClassInvocation} methods in
 * subclasses.
 *
 * <p>Similarly, {@code @BeforeParameterizedClassInvocation} methods declared in
 * an interface are inherited as long as they are not overridden, and
 * {@code @BeforeParameterizedClassInvocation} methods from an interface will be
 * executed before {@code @BeforeParameterizedClassInvocation} methods in the
 * class that implements the interface.
 *
 * <p>JUnit Jupiter does not guarantee the execution order of multiple
 * {@code @BeforeParameterizedClassInvocation} methods that are declared within
 * a single parameterized test class or test interface. While it may at times
 * appear that these methods are invoked in alphabetical order, they are in fact
 * sorted using an algorithm that is deterministic but intentionally
 * non-obvious.
 *
 * <p>In addition, {@code @BeforeParameterizedClassInvocation} methods are in no
 * way linked to {@code @AfterParameterizedClassInvocation} methods.
 * Consequently, there are no guarantees with regard to their <em>wrapping</em>
 * behavior. For example, given two {@code @BeforeParameterizedClassInvocation}
 * methods {@code createA()} and {@code createB()} as well as two
 * {@code @AfterParameterizedClassInvocation} methods {@code destroyA()} and
 * {@code destroyB()}, the order in which the
 * {@code @BeforeParameterizedClassInvocation} methods are executed (e.g.
 * {@code createA()} before {@code createB()}) does not imply any order for the
 * seemingly corresponding {@code @AfterParameterizedClassInvocation} methods.
 * In other words, {@code destroyA()} might be called before <em>or</em> after
 * {@code destroyB()}. The JUnit Team therefore recommends that developers
 * declare at most one {@code @BeforeParameterizedClassInvocation} method and at
 * most one {@code @AfterParameterizedClassInvocation} method per test class or
 * test interface unless there are no dependencies between the
 * {@code @BeforeParameterizedClassInvocation} methods or between the
 * {@code @AfterParameterizedClassInvocation} methods.
 *
 * <h2>Composition</h2>
 *
 * <p>{@code @BeforeParameterizedClassInvocation} may be used as a
 * meta-annotation in order to create a custom <em>composed annotation</em> that
 * inherits the semantics of {@code @BeforeParameterizedClassInvocation}.
 *
 * @since 5.13
 * @see ParameterizedClass
 * @see AfterParameterizedClassInvocation
 * @see org.junit.jupiter.api.TestInstance
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = EXPERIMENTAL, since = "6.0")
@ClassTemplateInvocationLifecycleMethod(classTemplateAnnotation = ParameterizedClass.class, lifecycleMethodAnnotation = BeforeParameterizedClassInvocation.class)
public @interface BeforeParameterizedClassInvocation {

	/**
	 * Whether the arguments of the parameterized test class should be injected
	 * into the annotated method (defaults to {@code true}).
	 */
	boolean injectArguments() default true;

}
