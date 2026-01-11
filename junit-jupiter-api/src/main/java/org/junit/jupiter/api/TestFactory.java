/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.platform.commons.annotation.Testable;

/**
 * {@code @TestFactory} is used to signal that the annotated method is a
 * <em>test factory</em> method.
 *
 * <p>In contrast to {@link Test @Test} methods, a test factory is not itself
 * a test case but rather a factory for test cases.
 *
 * <p>{@code @TestFactory} methods must not be {@code private} or {@code static}
 * and must return a {@code Stream}, {@code Collection}, {@code Iterable},
 * {@code Iterator}, array of {@link DynamicNode} instances, or any type that
 * provides an {@link java.util.Iterator Iterator}-returning {@code iterator()}
 * method (such as, for example, a {@code kotlin.sequences.Sequence}). Supported
 * subclasses of {@code DynamicNode} include {@link DynamicContainer} and
 * {@link DynamicTest}. <em>Dynamic tests</em> will be executed lazily,
 * enabling dynamic and even non-deterministic generation of test cases.
 *
 * <p>Any {@code Stream} returned by a {@code @TestFactory} will be properly
 * closed by calling {@code stream.close()}, making it safe to use a resource
 * such as {@code Files.lines()} as the initial source of the stream.
 *
 * <p>{@code @TestFactory} methods may optionally declare parameters to be
 * resolved by {@link org.junit.jupiter.api.extension.ParameterResolver
 * ParameterResolvers}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>{@code @TestFactory} methods are inherited from superclasses as long as
 * they are not <em>overridden</em> according to the visibility rules of the Java
 * language. Similarly, {@code @TestFactory} methods declared as <em>interface
 * default methods</em> are inherited as long as they are not overridden.
 *
 * <h2>Test Execution Order</h2>
 *
 * <p>By default, test methods will be ordered using an algorithm that is
 * deterministic but intentionally nonobvious. This ensures that subsequent runs
 * of a test suite execute test methods in the same order, thereby allowing for
 * repeatable builds. In this context, a <em>test method</em> is any instance
 * method that is directly annotated or meta-annotated with {@code @Test},
 * {@code @RepeatedTest}, {@code @ParameterizedTest}, {@code @TestFactory}, or
 * {@code @TestTemplate}.
 *
 * <p>Although true <em>unit tests</em> typically should not rely on the order
 * in which they are executed, there are times when it is necessary to enforce
 * a specific test method execution order &mdash; for example, when writing
 * <em>integration tests</em> or <em>functional tests</em> where the sequence of
 * the tests is important, especially in conjunction with
 * {@link TestInstance @TestInstance(Lifecycle.PER_CLASS)}.
 *
 * <p>To control the order in which test methods are executed, annotate your
 * test class or test interface with {@link TestMethodOrder @TestMethodOrder}
 * and specify the desired {@link MethodOrderer} implementation.
 *
 * @since 5.0
 * @see Test
 * @see DynamicNode
 * @see DynamicTest
 * @see DynamicContainer
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = MAINTAINED, since = "5.3")
@Testable
public @interface TestFactory {
}
