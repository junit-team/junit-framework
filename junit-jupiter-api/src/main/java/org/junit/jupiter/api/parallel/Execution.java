/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.parallel;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;

/**
 * {@code @Execution} is used to configure the parallel execution
 * {@linkplain #value mode} of a test class or test method.
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * <h2>Default Execution Mode</h2>
 *
 * <p>If this annotation is not present, {@link ExecutionMode#SAME_THREAD} is
 * used unless a default execution mode is defined via one of the following
 * configuration parameters:
 *
 * <dl>
 *     <dt>{@value #DEFAULT_EXECUTION_MODE_PROPERTY_NAME}</dt>
 *     <dd>Default execution mode for all classes and tests</dd>
 *     <dt>{@value #DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME}</dt>
 *     <dd>Default execution mode for top-level classes</dd>
 * </dl>
 *
 * <p>{@value #DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME} overrides
 * {@value #DEFAULT_EXECUTION_MODE_PROPERTY_NAME} for top-level classes.
 *
 * <p>The default execution mode is not applied to classes that use the
 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle or a
 * {@link MethodOrderer}. In both cases, test methods in such test classes are
 * only executed concurrently if the {@code @Execution(CONCURRENT)} annotation
 * is present on the test class or method.
 *
 * @see Isolated
 * @see ResourceLock
 * @since 5.3
 */
@API(status = STABLE, since = "5.10")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
public @interface Execution {

	/**
	 * Property name used to set the default test execution mode: {@value}
	 *
	 * <p>This setting is only effective if parallel execution is enabled.
	 *
	 * <h4>Supported Values</h4>
	 *
	 * <p>Supported values include names of enum constants defined in
	 * {@link ExecutionMode}, ignoring case.
	 *
	 * <p>If not specified, the default is "same_thread" which corresponds to
	 * {@code @Execution(ExecutionMode.SAME_THREAD)}.
	 *
	 * @since 5.4
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	String DEFAULT_EXECUTION_MODE_PROPERTY_NAME = "junit.jupiter.execution.parallel.mode.default";

	/**
	 * Property name used to set the default test execution mode for top-level
	 * classes: {@value}
	 *
	 * <p>This setting is only effective if parallel execution is enabled.
	 *
	 * <h4>Supported Values</h4>
	 *
	 * <p>Supported values include names of enum constants defined in
	 * {@link ExecutionMode}, ignoring case.
	 *
	 * <p>If not specified, it will be resolved into the same value as
	 * {@link #DEFAULT_EXECUTION_MODE_PROPERTY_NAME}.
	 *
	 * @since 5.4
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	String DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME = "junit.jupiter.execution.parallel.mode.classes.default";

	/**
	 * The required/preferred execution mode.
	 *
	 * @see ExecutionMode
	 */
	ExecutionMode value();

	/**
	 * The reason for using the selected execution mode.
	 *
	 * <p>This is for informational purposes only.
	 *
	 * @since 5.10
	 */
	@API(status = STABLE, since = "5.10")
	String reason() default "";

}
