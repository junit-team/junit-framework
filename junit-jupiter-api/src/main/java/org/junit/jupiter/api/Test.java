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

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.platform.commons.annotation.Testable;

/**
 * {@code @Test} is used to signal that the annotated method is a <em>test</em>
 * method.
 *
 * <p>{@code @Test} methods must not be {@code private} or {@code static} and
 * must not return a value.
 *
 * <p>{@code @Test} methods may optionally declare parameters to be resolved by
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolvers}.
 *
 * <p>{@code @Test} may also be used as a meta-annotation in order to create a
 * custom <em>composed annotation</em> that inherits the semantics of {@code @Test}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>{@code @Test} methods are inherited from superclasses as long as they are
 * not <em>overridden</em> according to the visibility rules of the Java language.
 * Similarly, {@code @Test} methods declared as <em>interface default methods</em>
 * are inherited as long as they are not overridden.
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
 * @see RepeatedTest
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see TestTemplate
 * @see TestFactory
 * @see TestInfo
 * @see DisplayName
 * @see Tag
 * @see BeforeAll
 * @see AfterAll
 * @see BeforeEach
 * @see AfterEach
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.0")
@Testable
public @interface Test {
}
