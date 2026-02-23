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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @RetryTest} is used to signal that the annotated method is a
 * <em>test template</em> method that should be retried up to {@linkplain #maxAttempts
 * a specified number of times} when it fails, with a configurable
 * {@linkplain #name display name} and an optional {@linkplain #delayMs delay}
 * between retry attempts.
 *
 * <p>Unlike {@link RepeatedTest @RepeatedTest}, which always executes all
 * repetitions, {@code @RetryTest} stops as soon as the test succeeds. The test
 * is considered successful if <em>any</em> attempt passes, and is only marked
 * as failed if <em>all</em> attempts fail.
 *
 * <p>This is particularly useful for dealing with <em>flaky</em> tests that
 * may fail intermittently due to non-deterministic factors such as network
 * latency, timing issues, or external service availability.
 *
 * <p>Each invocation of the retry test behaves like the execution of a
 * regular {@link Test @Test} method with full support for the same lifecycle
 * callbacks and extensions. In addition, the current attempt number and total
 * number of attempts can be accessed by having the {@link RetryInfo} injected.
 *
 * <p>{@code @RetryTest} methods must not be {@code private} or {@code static}
 * and must return {@code void}.
 *
 * <p>{@code @RetryTest} methods may optionally declare parameters to be
 * resolved by {@link org.junit.jupiter.api.extension.ParameterResolver
 * ParameterResolvers}.
 *
 * <p>{@code @RetryTest} may also be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics
 * of {@code @RetryTest}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>{@code @RetryTest} methods are inherited from superclasses as long as
 * they are not <em>overridden</em> according to the visibility rules of the Java
 * language. Similarly, {@code @RetryTest} methods declared as <em>interface
 * default methods</em> are inherited as long as they are not overridden.
 *
 * <h2>Test Execution Order</h2>
 *
 * <p>By default, test methods will be ordered using an algorithm that is
 * deterministic but intentionally nonobvious. This ensures that subsequent runs
 * of a test suite execute test methods in the same order, thereby allowing for
 * repeatable builds. In this context, a <em>test method</em> is any instance
 * method that is directly annotated or meta-annotated with {@code @Test},
 * {@code @RepeatedTest}, {@code @RetryTest}, {@code @ParameterizedTest},
 * {@code @TestFactory}, or {@code @TestTemplate}.
 *
 * <p>Although true <em>unit tests</em> typically should not rely on the order
 * in which they are executed, there are times when it is necessary to enforce
 * a specific test method execution order &mdash; for example, when writing
 * <em>integration tests</em> or <em>functional tests</em> where the sequence of
 * the tests is important, especially in conjunction with
 * {@link TestInstance @TestInstance(Lifecycle.PER_CLASS)}.
 *
 * <p>To control the order in which test methods are executed, annotate your
 * test class or test interface with {@link TestMethodOrder @TestMethodOrder}
 * and specify the desired {@link MethodOrderer} implementation.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * @RetryTest(maxAttempts = 3)
 * void flakyNetworkTest() {
 *     // This test will be retried up to 3 times if it fails
 *     var response = httpClient.get("https://example.com/api");
 *     assertEquals(200, response.statusCode());
 * }
 *
 * @RetryTest(maxAttempts = 5, delayMs = 1000)
 * void flakyTestWithDelay(RetryInfo retryInfo) {
 *     // Access retry information
 *     System.out.println("Attempt " + retryInfo.getCurrentAttempt()
 *         + " of " + retryInfo.getMaxAttempts());
 *     // ...
 * }
 * }</pre>
 *
 * <p><strong>WARNING</strong>: if the attempts of a {@code @RetryTest}
 * method are executed in parallel, no guarantees can be made regarding the
 * retry behavior. It is therefore recommended that a {@code @RetryTest}
 * method be annotated with
 * {@link org.junit.jupiter.api.parallel.Execution @Execution(SAME_THREAD)}
 * when parallel execution is configured.
 *
 * @since 6.1
 * @see DisplayName
 * @see RetryInfo
 * @see TestTemplate
 * @see TestInfo
 * @see Test
 * @see RepeatedTest
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = EXPERIMENTAL, since = "6.1")
@TestTemplate
public @interface RetryTest {

	/**
	 * Placeholder for the {@linkplain TestInfo#getDisplayName display name} of
	 * a {@code @RetryTest} method: <code>{displayName}</code>
	 */
	String DISPLAY_NAME_PLACEHOLDER = "{displayName}";

	/**
	 * Placeholder for the current attempt count of a {@code @RetryTest}
	 * method: <code>{currentAttempt}</code>
	 */
	String CURRENT_ATTEMPT_PLACEHOLDER = "{currentAttempt}";

	/**
	 * Placeholder for the maximum number of attempts of a {@code @RetryTest}
	 * method: <code>{maxAttempts}</code>
	 */
	String MAX_ATTEMPTS_PLACEHOLDER = "{maxAttempts}";

	/**
	 * <em>Short</em> display name pattern for a retry test: {@value}
	 *
	 * @see #CURRENT_ATTEMPT_PLACEHOLDER
	 * @see #MAX_ATTEMPTS_PLACEHOLDER
	 * @see #LONG_DISPLAY_NAME
	 */
	String SHORT_DISPLAY_NAME = "attempt " + CURRENT_ATTEMPT_PLACEHOLDER + " of " + MAX_ATTEMPTS_PLACEHOLDER;

	/**
	 * <em>Long</em> display name pattern for a retry test: {@value}
	 *
	 * @see #DISPLAY_NAME_PLACEHOLDER
	 * @see #SHORT_DISPLAY_NAME
	 */
	String LONG_DISPLAY_NAME = DISPLAY_NAME_PLACEHOLDER + " :: " + SHORT_DISPLAY_NAME;

	/**
	 * The maximum number of attempts, including the initial invocation.
	 *
	 * <p>For example, {@code maxAttempts = 3} means the test will be executed
	 * once and retried up to 2 additional times if it fails.
	 *
	 * @return the maximum number of attempts; must be greater than zero
	 */
	int maxAttempts();

	/**
	 * The delay in milliseconds to wait between retry attempts.
	 *
	 * <p>This delay is applied before each retry attempt (i.e., not before
	 * the initial invocation). A value of {@code 0} means no delay.
	 *
	 * <p>Defaults to {@code 0}.
	 *
	 * @return the delay in milliseconds; must not be negative
	 */
	long delayMs() default 0;

	/**
	 * The display name for each attempt of the retry test.
	 *
	 * <h4>Supported placeholders</h4>
	 * <ul>
	 * <li>{@link #DISPLAY_NAME_PLACEHOLDER}</li>
	 * <li>{@link #CURRENT_ATTEMPT_PLACEHOLDER}</li>
	 * <li>{@link #MAX_ATTEMPTS_PLACEHOLDER}</li>
	 * </ul>
	 *
	 * <p>Defaults to {@link #SHORT_DISPLAY_NAME}, resulting in
	 * names such as {@code "attempt 1 of 3"}, {@code "attempt 2 of 3"},
	 * etc.
	 *
	 * <p>Can be set to <code>{@link #LONG_DISPLAY_NAME}</code>, resulting in
	 * names such as {@code "myRetryTest() :: attempt 1 of 3"},
	 * {@code "myRetryTest() :: attempt 2 of 3"}, etc.
	 *
	 * <p>Alternatively, you can provide a custom display name, optionally
	 * using the aforementioned placeholders.
	 *
	 * @return a custom display name; never blank or consisting solely of
	 * whitespace
	 * @see #SHORT_DISPLAY_NAME
	 * @see #LONG_DISPLAY_NAME
	 * @see #DISPLAY_NAME_PLACEHOLDER
	 * @see #CURRENT_ATTEMPT_PLACEHOLDER
	 * @see #MAX_ATTEMPTS_PLACEHOLDER
	 * @see TestInfo#getDisplayName()
	 */
	String name() default SHORT_DISPLAY_NAME;

}
