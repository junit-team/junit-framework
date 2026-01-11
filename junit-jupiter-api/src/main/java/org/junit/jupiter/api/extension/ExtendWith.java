/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

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
 * {@code @ExtendWith} is a {@linkplain Repeatable repeatable} annotation that
 * is used to register {@linkplain Extension extensions} for the annotated test
 * class, test interface, test method, parameter, or field.
 *
 * <p>Annotated parameters are supported in test class constructors, in test
 * methods, and in {@code @BeforeAll}, {@code @AfterAll}, {@code @BeforeEach},
 * and {@code @AfterEach} lifecycle methods.
 *
 * <p>{@code @ExtendWith} fields may be either {@code static} or non-static.
 *
 * <h2>Inheritance</h2>
 *
 * <p>{@code @ExtendWith} fields are inherited from superclasses. Furthermore,
 * {@code @ExtendWith} fields from superclasses will be registered before
 * {@code @ExtendWith} fields in subclasses unless {@code @Order} is used to
 * alter that behavior (see below).
 *
 * <h2>Registration Order</h2>
 *
 * <p>When {@code @ExtendWith} is present on a test class, test interface, or
 * test method or on a parameter in a test method or lifecycle method, the
 * corresponding extensions will be registered in the order in which the
 * {@code @ExtendWith} annotations are discovered. For example, if a test class
 * is annotated with {@code @ExtendWith(A.class)} and then with
 * {@code @ExtendWith(B.class)}, extension {@code A} will be registered before
 * extension {@code B}.
 *
 * <p>By default, if multiple extensions are registered on fields via
 * {@code @ExtendWith}, they will be ordered using an algorithm that is
 * deterministic but intentionally nonobvious. This ensures that subsequent runs
 * of a test suite execute extensions in the same order, thereby allowing for
 * repeatable builds. However, there are times when extensions need to be
 * registered in an explicit order. To achieve that, you can annotate
 * {@code @ExtendWith} fields with {@link org.junit.jupiter.api.Order @Order}.
 * Any {@code @ExtendWith} field not annotated with {@code @Order} will be
 * ordered using the {@link org.junit.jupiter.api.Order#DEFAULT default} order
 * value. Note that {@code @RegisterExtension} fields can also be ordered with
 * {@code @Order}, relative to {@code @ExtendWith} fields and other
 * {@code @RegisterExtension} fields.
 *
 * <h2>Supported Extension APIs</h2>
 *
 * <ul>
 * <li>{@link ExecutionCondition}</li>
 * <li>{@link InvocationInterceptor}</li>
 * <li>{@link BeforeAllCallback}</li>
 * <li>{@link AfterAllCallback}</li>
 * <li>{@link BeforeEachCallback}</li>
 * <li>{@link AfterEachCallback}</li>
 * <li>{@link BeforeTestExecutionCallback}</li>
 * <li>{@link AfterTestExecutionCallback}</li>
 * <li>{@link TestInstanceFactory}</li>
 * <li>{@link TestInstancePostProcessor}</li>
 * <li>{@link TestInstancePreConstructCallback}</li>
 * <li>{@link TestInstancePreDestroyCallback}</li>
 * <li>{@link ParameterResolver}</li>
 * <li>{@link LifecycleMethodExecutionExceptionHandler}</li>
 * <li>{@link TestExecutionExceptionHandler}</li>
 * <li>{@link TestTemplateInvocationContextProvider}</li>
 * <li>{@link TestWatcher}</li>
 * </ul>
 *
 * @since 5.0
 * @see RegisterExtension
 * @see Extension
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(Extensions.class)
@API(status = STABLE, since = "5.0")
public @interface ExtendWith {

	/**
	 * An array of one or more {@link Extension} classes to register.
	 */
	Class<? extends Extension>[] value();

}
