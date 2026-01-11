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

/**
 * {@code @AutoClose} is used to indicate that an annotated field will be
 * automatically closed after test execution.
 *
 * <p>{@code @AutoClose} fields may be either {@code static} or non-static. If
 * the value of an {@code @AutoClose} field is {@code null} when it is evaluated
 * the field will be ignored, but a warning message will be logged to inform you.
 *
 * <p>By default, {@code @AutoClose} expects the value of the annotated field to
 * implement a {@code close()} method that will be invoked to close the resource.
 * However, developers can customize the name of the {@code close} method via the
 * {@link #value} attribute. For example, {@code @AutoClose("shutdown")} instructs
 * JUnit to look for a {@code shutdown()} method to close the resource.
 *
 * <p>{@code @AutoClose} may be used as a meta-annotation in order to create a
 * custom <em>composed annotation</em> that inherits the semantics of
 * {@code @AutoClose}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>{@code @AutoClose} fields are inherited from superclasses. Furthermore,
 * {@code @AutoClose} fields from subclasses will be closed before
 * {@code @AutoClose} fields in superclasses.
 *
 * <h2>Evaluation Order</h2>
 *
 * <p>When multiple {@code @AutoClose} fields exist within a given test class,
 * the order in which the resources are closed depends on an algorithm that is
 * deterministic but intentionally nonobvious. This ensures that subsequent runs
 * of a test suite close resources in the same order, thereby allowing for
 * repeatable builds.
 *
 * <h2>Scope and Lifecycle</h2>
 *
 * <p>The extension that closes {@code @AutoClose} fields implements the
 * {@link org.junit.jupiter.api.extension.AfterAllCallback AfterAllCallback} and
 * {@link org.junit.jupiter.api.extension.TestInstancePreDestroyCallback
 * TestInstancePreDestroyCallback} extension APIs. Consequently, a {@code static}
 * {@code @AutoClose} field will be closed after all tests in the current test
 * class have completed, effectively after {@code @AfterAll} methods have executed
 * for the test class. A non-static {@code @AutoClose} field will be closed before
 * the current test class instance is destroyed. Specifically, if the test class
 * is configured with
 * {@link TestInstance.Lifecycle#PER_METHOD @TestInstance(Lifecycle.PER_METHOD)}
 * semantics, a non-static {@code @AutoClose} field will be closed after the
 * execution of each test method, test factory method, or test template method.
 * However, if the test class is configured with
 * {@link TestInstance.Lifecycle#PER_CLASS @TestInstance(Lifecycle.PER_CLASS)}
 * semantics, a non-static {@code @AutoClose} field will not be closed until the
 * current test class instance is no longer needed, which means after
 * {@code @AfterAll} methods and after all {@code static} {@code @AutoClose} fields
 * have been closed.
 *
 * @since 5.11
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = MAINTAINED, since = "5.13.3")
public @interface AutoClose {

	/**
	 * Specify the name of the method to invoke to close the resource.
	 *
	 * <p>The default value is {@code "close"} which works with any type that
	 * implements {@link AutoCloseable} or has a {@code close()} method.
	 *
	 * @return the name of the method to invoke to close the resource
	 */
	String value() default "close";

}
