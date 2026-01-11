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
import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.apiguardian.api.API;

/**
 * {@code @Timeout} is used to define a timeout for a method or all testable
 * methods within one class and its {@link Nested @Nested} classes.
 *
 * <p>This annotation may also be used on lifecycle methods annotated with
 * {@link BeforeAll @BeforeAll}, {@link BeforeEach @BeforeEach},
 * {@link AfterEach @AfterEach}, or {@link AfterAll @AfterAll}.
 *
 * <p>Applying this annotation to a test class has the same effect as applying
 * it to all testable methods, i.e. all methods annotated or meta-annotated with
 * {@link Test @Test}, {@link TestFactory @TestFactory}, or
 * {@link TestTemplate @TestTemplate}, but not to its lifecycle methods.
 *
 * <h2>Default Timeouts</h2>
 *
 * <p>If this annotation is not present, no timeout will be used unless a
 * default timeout is defined via one of the following configuration parameters:
 *
 * <dl>
 *     <dt>{@value #DEFAULT_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for all testable and lifecycle methods</dd>
 *     <dt>{@value #DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for all testable methods</dd>
 *     <dt>{@value #DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for {@link Test @Test} methods</dd>
 *     <dt>{@value #DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for {@link TestTemplate @TestTemplate} methods</dd>
 *     <dt>{@value #DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for {@link TestFactory @TestFactory} methods</dd>
 *     <dt>{@value #DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for all lifecycle methods</dd>
 *     <dt>{@value #DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for {@link BeforeAll @BeforeAll} methods</dd>
 *     <dt>{@value #DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for {@link BeforeEach @BeforeEach} methods</dd>
 *     <dt>{@value #DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for {@link AfterEach @AfterEach} methods</dd>
 *     <dt>{@value #DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME}</dt>
 *     <dd>Default timeout for {@link AfterAll @AfterAll} methods</dd>
 * </dl>
 *
 * <p>More specific configuration parameters override less specific ones. For
 * example, {@value #DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME}
 * overrides {@value #DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME}
 * which overrides {@value #DEFAULT_TIMEOUT_PROPERTY_NAME}.
 *
 * <h3 id="supported-values">Supported Values</h3>
 *
 * <p>Values for timeouts must be in the following, case-insensitive format:
 * {@code <number> [ns|μs|ms|s|m|h|d]}. The space between the number and the
 * unit may be omitted. Specifying no unit is equivalent to using seconds.
 *
 * <table class="plain">
 * <caption>Timeout configuration via configuration parameter vs. annotation</caption>
 * <tr><th> Value         </th><th> Equivalent annotation                             </th></tr>
 * <tr><td> {@code 42}    </td><td> {@code @Timeout(42)}                              </td></tr>
 * <tr><td> {@code 42 ns} </td><td> {@code @Timeout(value = 42, unit = NANOSECONDS)}  </td></tr>
 * <tr><td> {@code 42 μs} </td><td> {@code @Timeout(value = 42, unit = MICROSECONDS)} </td></tr>
 * <tr><td> {@code 42 ms} </td><td> {@code @Timeout(value = 42, unit = MILLISECONDS)} </td></tr>
 * <tr><td> {@code 42 s}  </td><td> {@code @Timeout(value = 42, unit = SECONDS)}      </td></tr>
 * <tr><td> {@code 42 m}  </td><td> {@code @Timeout(value = 42, unit = MINUTES)}      </td></tr>
 * <tr><td> {@code 42 h}  </td><td> {@code @Timeout(value = 42, unit = HOURS)}        </td></tr>
 * <tr><td> {@code 42 d}  </td><td> {@code @Timeout(value = 42, unit = DAYS)}         </td></tr>
 * </table>
 *
 * <h2>Disabling Timeouts</h2>
 *
 * <p>You may use the {@value #TIMEOUT_MODE_PROPERTY_NAME} configuration
 * parameter to explicitly enable or disable timeouts.
 *
 * <p>Supported values:
 * <ul>
 * <li>{@code enabled}: enables timeouts
 * <li>{@code disabled}: disables timeouts
 * <li>{@code disabled_on_debug}: disables timeouts while debugging
 * </ul>
 *
 * @since 5.5
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@API(status = STABLE, since = "5.7")
public @interface Timeout {

	/**
	 * Property name used to set the default timeout for all testable and
	 * lifecycle methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a more
	 * specific property or a {@link Timeout @Timeout}
	 * annotation present on the method or on an enclosing test class (for
	 * testable methods).
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.default";

	/**
	 * Property name used to set the default timeout for all testable methods:
	 * {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a more
	 * specific property or a {@link Timeout @Timeout}
	 * annotation present on the testable method or on an enclosing test class.
	 *
	 * <p>This property overrides the {@value #DEFAULT_TIMEOUT_PROPERTY_NAME}
	 * property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.testable.method.default";

	/**
	 * Property name used to set the default timeout for all {@link Test @Test}
	 * methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a
	 * {@link Timeout @Timeout} annotation present on the {@link Test @Test}
	 * method or on an enclosing test class.
	 *
	 * <p>This property overrides the
	 * {@value #DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME} property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.test.method.default";

	/**
	 * Property name used to set the default timeout for all
	 * {@link TestTemplate @TestTemplate} methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a
	 * {@link Timeout @Timeout} annotation present on the
	 * {@link TestTemplate @TestTemplate} method or on an enclosing test class.
	 *
	 * <p>This property overrides the
	 * {@value #DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME} property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.testtemplate.method.default";

	/**
	 * Property name used to set the default timeout for all
	 * {@link TestFactory @TestFactory} methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a
	 * {@link Timeout @Timeout} annotation present on the
	 * {@link TestFactory @TestFactory} method or on an enclosing test class.
	 *
	 * <p>This property overrides the
	 * {@value #DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME} property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.testfactory.method.default";

	/**
	 * Property name used to set the default timeout for all lifecycle methods:
	 * {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a more
	 * specific property or a {@link Timeout @Timeout} annotation present on the
	 * lifecycle method.
	 *
	 * <p>This property overrides the {@value #DEFAULT_TIMEOUT_PROPERTY_NAME}
	 * property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.lifecycle.method.default";

	/**
	 * Property name used to set the default timeout for all
	 * {@link BeforeAll @BeforeAll} methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a
	 * {@link Timeout @Timeout} annotation present on the
	 * {@link BeforeAll @BeforeAll} method.
	 *
	 * <p>This property overrides the
	 * {@value #DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME} property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.beforeall.method.default";

	/**
	 * Property name used to set the default timeout for all
	 * {@link BeforeEach @BeforeEach} methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a
	 * {@link Timeout @Timeout} annotation present on the
	 * {@link BeforeEach @BeforeEach} method.
	 *
	 * <p>This property overrides the
	 * {@value #DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME} property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.beforeeach.method.default";

	/**
	 * Property name used to set the default timeout for all
	 * {@link AfterEach @AfterEach} methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a
	 * {@link Timeout @Timeout} annotation present on the
	 * {@link AfterEach @AfterEach} method.
	 *
	 * <p>This property overrides the
	 * {@value #DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME} property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.aftereach.method.default";

	/**
	 * Property name used to set the default timeout for all
	 * {@link AfterAll @AfterAll} methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a
	 * {@link Timeout @Timeout} annotation present on the
	 * {@link AfterAll @AfterAll} method.
	 *
	 * <p>This property overrides the
	 * {@value #DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME} property.
	 *
	 * <p>Please refer to the <a href="#supported-values">class
	 * description</a> for the definition of supported values.
	 *
	 * @since 5.5
	 */
	@API(status = STABLE, since = "5.9")
	String DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME = "junit.jupiter.execution.timeout.afterall.method.default";

	/**
	 * Property name used to configure whether timeouts are applied to tests:
	 * {@value}.
	 *
	 * <p>The value of this property will be used to toggle whether
	 * {@link Timeout @Timeout} is applied to tests.</p>
	 *
	 * <h4>Supported timeout mode values (case insensitive):</h4>
	 * <ul>
	 * <li>{@code ENABLED}: enables timeouts
	 * <li>{@code DISABLED}: disables timeouts
	 * <li>{@code DISABLED_ON_DEBUG}: disables timeouts while debugging
	 * </ul>
	 *
	 * <p>If not specified, the default is {@code ENABLED}.
	 *
	 * @since 5.6
	 */
	@API(status = STABLE, since = "5.9")
	String TIMEOUT_MODE_PROPERTY_NAME = "junit.jupiter.execution.timeout.mode";

	/**
	 * Property name used to set the default thread mode for all testable and
	 * lifecycle methods: {@value}.
	 *
	 * <p>The value of this property will be used unless overridden by a
	 * {@link Timeout @Timeout} annotation present on the method or on an
	 * enclosing test class (for testable methods).
	 *
	 * <p>The supported values are {@code SAME_THREAD} or
	 * {@code SEPARATE_THREAD}, ignoring case. If none is provided,
	 * {@code SAME_THREAD} is used as default.
	 *
	 * @since 5.9
	 * @see #threadMode()
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	String DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME = "junit.jupiter.execution.timeout.thread.mode.default";

	/**
	 * The duration of this timeout.
	 *
	 * @return timeout duration; must be a positive number
	 */
	long value();

	/**
	 * The time unit of this timeout.
	 *
	 * @return time unit
	 * @see TimeUnit
	 */
	TimeUnit unit() default TimeUnit.SECONDS;

	/**
	 * The thread mode of this timeout.
	 *
	 * @return thread mode
	 * @since 5.9
	 * @see ThreadMode
	 * @see #DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME
	 */
	@API(status = STABLE, since = "5.11")
	ThreadMode threadMode() default ThreadMode.INFERRED;

	/**
	 * {@code ThreadMode} is used to define whether test code should be executed
	 * in the thread of the calling code or in a separate thread.
	 *
	 * @since 5.9
	 */
	@API(status = STABLE, since = "5.11")
	enum ThreadMode {
		/**
		 * The thread mode is determined using the parameter configured in property
		 * {@value Timeout#DEFAULT_TIMEOUT_THREAD_MODE_PROPERTY_NAME}.
		 */
		INFERRED,

		/**
		 * The test code is executed in the thread of the calling code.
		 */
		SAME_THREAD,

		/**
		 * The test code is executed in a different thread than that of the calling code. Furthermore,
		 * execution of the test code will be preemptively aborted if the timeout is exceeded. See the
		 * {@linkplain Assertions Preemptive Timeouts} section of the class-level
		 * Javadoc for a discussion of possible undesirable side effects.
		 */
		SEPARATE_THREAD,
	}

}
