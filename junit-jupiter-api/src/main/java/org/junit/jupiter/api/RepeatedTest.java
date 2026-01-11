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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @RepeatedTest} is used to signal that the annotated method is a
 * <em>test template</em> method that should be repeated a {@linkplain #value
 * specified number of times} with a configurable {@linkplain #name display
 * name} and an optional {@linkplain #failureThreshold() failure threshold}.
 *
 * <p>Each invocation of the repeated test behaves like the execution of a
 * regular {@link Test @Test} method with full support for the same lifecycle
 * callbacks and extensions. In addition, the current repetition and total
 * number of repetitions can be accessed by having the {@link RepetitionInfo}
 * injected.
 *
 * <p>{@code @RepeatedTest} methods must not be {@code private} or {@code static}
 * and must return {@code void}.
 *
 * <p>{@code @RepeatedTest} methods may optionally declare parameters to be
 * resolved by {@link org.junit.jupiter.api.extension.ParameterResolver
 * ParameterResolvers}.
 *
 * <p>{@code @RepeatedTest} may also be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics
 * of {@code @RepeatedTest}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>{@code @RepeatedTest} methods are inherited from superclasses as long as
 * they are not <em>overridden</em> according to the visibility rules of the Java
 * language. Similarly, {@code @RepeatedTest} methods declared as <em>interface
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
 * @see DisplayName
 * @see RepetitionInfo
 * @see TestTemplate
 * @see TestInfo
 * @see Test
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.0")
@TestTemplate
public @interface RepeatedTest {

	/**
	 * Placeholder for the {@linkplain TestInfo#getDisplayName display name} of
	 * a {@code @RepeatedTest} method: <code>{displayName}</code>
	 */
	String DISPLAY_NAME_PLACEHOLDER = "{displayName}";

	/**
	 * Placeholder for the current repetition count of a {@code @RepeatedTest}
	 * method: <code>{currentRepetition}</code>
	 */
	String CURRENT_REPETITION_PLACEHOLDER = "{currentRepetition}";

	/**
	 * Placeholder for the total number of repetitions of a {@code @RepeatedTest}
	 * method: <code>{totalRepetitions}</code>
	 */
	String TOTAL_REPETITIONS_PLACEHOLDER = "{totalRepetitions}";

	/**
	 * <em>Short</em> display name pattern for a repeated test: {@value}
	 *
	 * @see #CURRENT_REPETITION_PLACEHOLDER
	 * @see #TOTAL_REPETITIONS_PLACEHOLDER
	 * @see #LONG_DISPLAY_NAME
	 */
	String SHORT_DISPLAY_NAME = "repetition " + CURRENT_REPETITION_PLACEHOLDER + " of " + TOTAL_REPETITIONS_PLACEHOLDER;

	/**
	 * <em>Long</em> display name pattern for a repeated test: {@value}
	 *
	 * @see #DISPLAY_NAME_PLACEHOLDER
	 * @see #SHORT_DISPLAY_NAME
	 */
	String LONG_DISPLAY_NAME = DISPLAY_NAME_PLACEHOLDER + " :: " + SHORT_DISPLAY_NAME;

	/**
	 * The number of repetitions.
	 *
	 * @return the number of repetitions; must be greater than zero
	 */
	int value();

	/**
	 * The display name for each repetition of the repeated test.
	 *
	 * <h4>Supported placeholders</h4>
	 * <ul>
	 * <li>{@link #DISPLAY_NAME_PLACEHOLDER}</li>
	 * <li>{@link #CURRENT_REPETITION_PLACEHOLDER}</li>
	 * <li>{@link #TOTAL_REPETITIONS_PLACEHOLDER}</li>
	 * </ul>
	 *
	 * <p>Defaults to {@link #SHORT_DISPLAY_NAME}, resulting in
	 * names such as {@code "repetition 1 of 2"}, {@code "repetition 2 of 2"},
	 * etc.
	 *
	 * <p>Can be set to <code>{@link #LONG_DISPLAY_NAME}</code>, resulting in
	 * names such as {@code "myRepeatedTest() :: repetition 1 of 2"},
	 * {@code "myRepeatedTest() :: repetition 2 of 2"}, etc.
	 *
	 * <p>Alternatively, you can provide a custom display name, optionally
	 * using the aforementioned placeholders.
	 *
	 * @return a custom display name; never blank or consisting solely of
	 * whitespace
	 * @see #SHORT_DISPLAY_NAME
	 * @see #LONG_DISPLAY_NAME
	 * @see #DISPLAY_NAME_PLACEHOLDER
	 * @see #CURRENT_REPETITION_PLACEHOLDER
	 * @see #TOTAL_REPETITIONS_PLACEHOLDER
	 * @see TestInfo#getDisplayName()
	 */
	String name() default SHORT_DISPLAY_NAME;

	/**
	 * Configures the number of failures after which remaining repetitions will
	 * be automatically skipped.
	 *
	 * <p>Set this to a positive number less than the total {@linkplain #value()
	 * number of repetitions} in order to skip the invocations of remaining
	 * repetitions after the specified number of failures has been encountered.
	 *
	 * <p>For example, if you are using {@code @RepeatedTest} to repeatedly invoke
	 * a test that you suspect to be <em>flaky</em>, a single failure is sufficient
	 * to demonstrate that the test is flaky, and there is no need to invoke the
	 * remaining repetitions. To support that specific use case, set
	 * {@code failureThreshold = 1}. You can alternatively set the threshold to
	 * a number greater than {@code 1} depending on your use case.
	 *
	 * <p>Defaults to {@link Integer#MAX_VALUE}, signaling that no failure
	 * threshold will be applied, which effectively means that the specified
	 * {@linkplain #value() number of repetitions} will be invoked regardless of
	 * whether any repetitions fail.
	 *
	 * <p><strong>WARNING</strong>: if the repetitions of a {@code @RepeatedTest}
	 * method are executed in parallel, no guarantees can be made regarding the
	 * failure threshold. It is therefore recommended that a {@code @RepeatedTest}
	 * method be annotated with
	 * {@link org.junit.jupiter.api.parallel.Execution @Execution(SAME_THREAD)}
	 * when parallel execution is configured.
	 *
	 * @since 5.10
	 * @return the failure threshold; must be greater than zero and less than the
	 * total number of repetitions
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	int failureThreshold() default Integer.MAX_VALUE;

}
