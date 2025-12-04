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

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * {@code @ParameterizedTest} is used to signal that the annotated method is a
 * <em>parameterized test</em> method.
 *
 * <p>Such methods must not be {@code private} or {@code static}.
 *
 * <h2>Arguments Providers and Sources</h2>
 *
 * <p>{@code @ParameterizedTest} methods must specify at least one
 * {@link org.junit.jupiter.params.provider.ArgumentsProvider ArgumentsProvider}
 * via {@link org.junit.jupiter.params.provider.ArgumentsSource @ArgumentsSource}
 * or a corresponding composed annotation (e.g., {@code @ValueSource},
 * {@code @CsvSource}, etc.). The provider is responsible for providing a
 * {@link java.util.stream.Stream Stream} of
 * {@link org.junit.jupiter.params.provider.Arguments Arguments} that will be
 * used to invoke the parameterized test method.
 *
 * <h2>Formal Parameter List</h2>
 *
 * <p>A {@code @ParameterizedTest} method may declare additional parameters at
 * the end of the method's parameter list to be resolved by other
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolvers}
 * (e.g., {@code TestInfo}, {@code TestReporter}, etc.). Specifically, a
 * parameterized test method must declare formal parameters according to the
 * following rules.
 *
 * <ol>
 * <li>Zero or more <em>indexed parameters</em> must be declared first.</li>
 * <li>Zero or more <em>aggregators</em> must be declared next.</li>
 * <li>Zero or more parameters supplied by other {@code ParameterResolver}
 * implementations must be declared last.</li>
 * </ol>
 *
 * <p>In this context, an <em>indexed parameter</em> is an argument for a given
 * index in the {@code Arguments} provided by an {@code ArgumentsProvider} that
 * is passed as an argument to the parameterized method at the same index in the
 * method's formal parameter list. An <em>aggregator</em> is any parameter of type
 * {@link org.junit.jupiter.params.aggregator.ArgumentsAccessor ArgumentsAccessor}
 * or any parameter annotated with
 * {@link org.junit.jupiter.params.aggregator.AggregateWith @AggregateWith}.
 *
 * <h2>Argument Conversion</h2>
 *
 * <p>Method parameters may be annotated with
 * {@link org.junit.jupiter.params.converter.ConvertWith @ConvertWith}
 * or a corresponding composed annotation to specify an <em>explicit</em>
 * {@link org.junit.jupiter.params.converter.ArgumentConverter ArgumentConverter}.
 * Otherwise, JUnit Jupiter will attempt to perform an <em>implicit</em>
 * conversion to the target type automatically (see the User Guide for further
 * details).
 *
 * <h2>Composed Annotations</h2>
 *
 * <p>{@code @ParameterizedTest} may also be used as a meta-annotation in order
 * to create a custom <em>composed annotation</em> that inherits the semantics
 * of {@code @ParameterizedTest}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>{@code @ParameterizedTest} methods are inherited from superclasses as long
 * as they are not <em>overridden</em> according to the visibility rules of the
 * Java language. Similarly, {@code @ParameterizedTest} methods declared as
 * <em>interface default methods</em> are inherited as long as they are not
 * overridden.
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
 * {@link org.junit.jupiter.api.TestInstance @TestInstance(Lifecycle.PER_CLASS)}.
 *
 * <p>To control the order in which test methods are executed, annotate your
 * test class or test interface with
 * {@link org.junit.jupiter.api.TestMethodOrder @TestMethodOrder} and specify
 * the desired {@link org.junit.jupiter.api.MethodOrderer MethodOrderer}
 * implementation.
 *
 * @since 5.0
 * @see ParameterizedClass
 * @see org.junit.jupiter.params.provider.Arguments
 * @see org.junit.jupiter.params.provider.ArgumentsProvider
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.provider.CsvFileSource
 * @see org.junit.jupiter.params.provider.CsvSource
 * @see org.junit.jupiter.params.provider.EnumSource
 * @see org.junit.jupiter.params.provider.MethodSource
 * @see org.junit.jupiter.params.provider.ValueSource
 * @see org.junit.jupiter.params.aggregator.ArgumentsAccessor
 * @see org.junit.jupiter.params.aggregator.AggregateWith
 * @see org.junit.jupiter.params.converter.ArgumentConverter
 * @see org.junit.jupiter.params.converter.ConvertWith
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.7")
@TestTemplate
@ExtendWith(ParameterizedTestExtension.class)
@SuppressWarnings("exports")
public @interface ParameterizedTest {

	/**
	 * See {@link ParameterizedInvocationConstants#DISPLAY_NAME_PLACEHOLDER}.
	 *
	 * @since 5.3
	 * @see #name
	 * @deprecated Please reference
	 * {@link ParameterizedInvocationConstants#DISPLAY_NAME_PLACEHOLDER}
	 * instead.
	 */
	@API(status = DEPRECATED, since = "5.13")
	@Deprecated(since = "5.13")
	String DISPLAY_NAME_PLACEHOLDER = ParameterizedInvocationConstants.DISPLAY_NAME_PLACEHOLDER;

	/**
	 * See {@link ParameterizedInvocationConstants#INDEX_PLACEHOLDER}.
	 *
	 * @since 5.3
	 * @see #name
	 * @see ParameterizedInvocationConstants#DEFAULT_DISPLAY_NAME
	 * @deprecated Please reference
	 * {@link ParameterizedInvocationConstants#INDEX_PLACEHOLDER} instead.
	 */
	@API(status = DEPRECATED, since = "5.13")
	@Deprecated(since = "5.13")
	String INDEX_PLACEHOLDER = ParameterizedInvocationConstants.INDEX_PLACEHOLDER;

	/**
	 * See {@link ParameterizedInvocationConstants#ARGUMENTS_PLACEHOLDER}.
	 *
	 * @since 5.3
	 * @see #name
	 * @deprecated Please reference
	 * {@link ParameterizedInvocationConstants#ARGUMENTS_PLACEHOLDER} instead.
	 */
	@API(status = DEPRECATED, since = "5.13")
	@Deprecated(since = "5.13")
	String ARGUMENTS_PLACEHOLDER = ParameterizedInvocationConstants.ARGUMENTS_PLACEHOLDER;

	/**
	 * See
	 * {@link ParameterizedInvocationConstants#ARGUMENTS_WITH_NAMES_PLACEHOLDER}.
	 *
	 * @since 5.6
	 * @see #name
	 * @see ParameterizedInvocationConstants#ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 * @deprecated Please reference
	 * {@link ParameterizedInvocationConstants#ARGUMENTS_WITH_NAMES_PLACEHOLDER}
	 * instead.
	 */
	@API(status = DEPRECATED, since = "5.13")
	@Deprecated(since = "5.13")
	String ARGUMENTS_WITH_NAMES_PLACEHOLDER = ParameterizedInvocationConstants.ARGUMENTS_WITH_NAMES_PLACEHOLDER;

	/**
	 * See
	 * {@link ParameterizedInvocationConstants#ARGUMENT_SET_NAME_PLACEHOLDER}.
	 *
	 * @since 5.11
	 * @see #name
	 * @see ParameterizedInvocationConstants#ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 * @see org.junit.jupiter.params.provider.Arguments#argumentSet(String, Object...)
	 * @deprecated Please reference
	 * {@link ParameterizedInvocationConstants#ARGUMENT_SET_NAME_PLACEHOLDER}
	 * instead.
	 */
	@API(status = DEPRECATED, since = "5.13")
	@Deprecated(since = "5.13")
	String ARGUMENT_SET_NAME_PLACEHOLDER = ParameterizedInvocationConstants.ARGUMENT_SET_NAME_PLACEHOLDER;

	/**
	 * See
	 * {@link ParameterizedInvocationConstants#ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER}.
	 *
	 * @since 5.11
	 * @see #name
	 * @see ParameterizedInvocationConstants#ARGUMENT_SET_NAME_PLACEHOLDER
	 * @see ParameterizedInvocationConstants#ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 * @see ParameterizedInvocationConstants#DEFAULT_DISPLAY_NAME
	 * @see org.junit.jupiter.params.provider.Arguments#argumentSet(String, Object...)
	 * @deprecated Please reference
	 * {@link ParameterizedInvocationConstants#ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER}
	 * instead.
	 */
	@API(status = DEPRECATED, since = "5.13")
	@Deprecated(since = "5.13")
	String ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER = //
		ParameterizedInvocationConstants.ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER;

	/**
	 * See
	 * {@link ParameterizedInvocationConstants#DEFAULT_DISPLAY_NAME}.
	 *
	 * @since 5.3
	 * @see #name
	 * @see ParameterizedInvocationConstants#DISPLAY_NAME_PLACEHOLDER
	 * @see ParameterizedInvocationConstants#INDEX_PLACEHOLDER
	 * @see ParameterizedInvocationConstants#ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER
	 * @deprecated Please reference
	 * {@link ParameterizedInvocationConstants#DEFAULT_DISPLAY_NAME} instead.
	 */
	@API(status = DEPRECATED, since = "5.13")
	@Deprecated(since = "5.13")
	String DEFAULT_DISPLAY_NAME = ParameterizedInvocationConstants.DEFAULT_DISPLAY_NAME;

	/**
	 * The display name to be used for individual invocations of the
	 * parameterized test; never blank or consisting solely of whitespace.
	 *
	 * <p>Defaults to <code>{@value ParameterizedInvocationNameFormatter#DEFAULT_DISPLAY_NAME}</code>.
	 *
	 * <p>If the default display name flag
	 * (<code>{@value ParameterizedInvocationNameFormatter#DEFAULT_DISPLAY_NAME}</code>)
	 * is not overridden, JUnit will:
	 * <ul>
	 * <li>Look up the {@value ParameterizedInvocationNameFormatter#DISPLAY_NAME_PATTERN_KEY}
	 * <em>configuration parameter</em> and use it if available. The configuration
	 * parameter can be supplied via the {@code Launcher} API, build tools (e.g.,
	 * Gradle and Maven), a JVM system property, or the JUnit Platform configuration
	 * file (i.e., a file named {@code junit-platform.properties} in the root of
	 * the class path). Consult the User Guide for further information.</li>
	 * <li>Otherwise, <code>{@value ParameterizedInvocationConstants#DEFAULT_DISPLAY_NAME}</code> will be used.</li>
	 * </ul>
	 *
	 * <h4>Supported placeholders</h4>
	 * <ul>
	 * <li><code>{@value ParameterizedInvocationConstants#DISPLAY_NAME_PLACEHOLDER}</code></li>
	 * <li><code>{@value ParameterizedInvocationConstants#INDEX_PLACEHOLDER}</code></li>
	 * <li><code>{@value ParameterizedInvocationConstants#ARGUMENT_SET_NAME_PLACEHOLDER}</code></li>
	 * <li><code>{@value ParameterizedInvocationConstants#ARGUMENTS_PLACEHOLDER}</code></li>
	 * <li><code>{@value ParameterizedInvocationConstants#ARGUMENTS_WITH_NAMES_PLACEHOLDER}</code></li>
	 * <li><code>{@value ParameterizedInvocationConstants#ARGUMENT_SET_NAME_OR_ARGUMENTS_WITH_NAMES_PLACEHOLDER}</code></li>
	 * <li><code>"{0}"</code>, <code>"{1}"</code>, etc.: an individual argument (0-based)</li>
	 * </ul>
	 *
	 * <p>For the latter, you may use {@link java.text.MessageFormat} patterns
	 * to customize formatting (for example, {@code {0,number,#.###}}). Please
	 * note that the original arguments are passed when formatting, regardless
	 * of any implicit or explicit argument conversions.
	 *
	 * <p>Note that
	 * <code>{@value ParameterizedInvocationNameFormatter#DEFAULT_DISPLAY_NAME}</code> is
	 * a flag rather than a placeholder.
	 *
	 * @see java.text.MessageFormat
	 * @see #quoteTextArguments()
	 */
	String name() default ParameterizedInvocationNameFormatter.DEFAULT_DISPLAY_NAME;

	/**
	 * Configure whether to enclose text-based argument values in quotes within
	 * display names.
	 *
	 * <p>Defaults to {@code true}.
	 *
	 * <p>In this context, any {@link CharSequence} (such as a {@link String})
	 * or {@link Character} is considered text. A {@code CharSequence} is wrapped
	 * in double quotes ("), and a {@code Character} is wrapped in single quotes
	 * (').
	 *
	 * <p>Special characters in Java strings and characters will be escaped in the
	 * quoted text &mdash; for example, carriage returns and line feeds will be
	 * escaped as {@code \\r} and {@code \\n}, respectively. In addition, any
	 * {@linkplain Character#isISOControl(char) ISO control character} will be
	 * represented as a question mark (?) in the quoted text.
	 *
	 * <p>For example, given a string argument {@code "line 1\nline 2"}, the
	 * representation in the display name would be {@code "\"line 1\\nline 2\""}
	 * (printed as {@code "line 1\nline 2"}) with the newline character escaped as
	 * {@code "\\n"}. Similarly, given a string argument {@code "\t"}, the
	 * representation in the display name would be {@code "\"\\t\""} (printed as
	 * {@code "\t"}) instead of a blank string or invisible tab
	 * character. The same applies for a character argument {@code '\t'}, whose
	 * representation in the display name would be {@code "'\\t'"} (printed as
	 * {@code '\t'}).
	 *
	 * <p>Please note that original source arguments are quoted when generating
	 * a display name, before any implicit or explicit argument conversion is
	 * performed. For example, if a parameterized test accepts {@code 3.14} as a
	 * {@code float} argument that was converted from {@code "3.14"} as an input
	 * string, {@code "3.14"} will be present in the display name instead of
	 * {@code 3.14}.
	 *
	 * @since 6.0
	 * @see #name()
	 */
	@API(status = EXPERIMENTAL, since = "6.0")
	boolean quoteTextArguments() default true;

	/**
	 * Configure whether all arguments of the parameterized test that implement
	 * {@link AutoCloseable} will be closed after their corresponding
	 * invocation.
	 *
	 * <p>Defaults to {@code true}.
	 *
	 * <p><strong>WARNING</strong>: if an argument that implements
	 * {@code AutoCloseable} is reused for multiple invocations of the same
	 * parameterized test method, you must set {@code autoCloseArguments} to
	 * {@code false} to ensure that the argument is not closed between
	 * invocations.
	 *
	 * @since 5.8
	 * @see java.lang.AutoCloseable
	 */
	@API(status = STABLE, since = "5.10")
	boolean autoCloseArguments() default true;

	/**
	 * Configure whether zero invocations are allowed for this
	 * parameterized test.
	 *
	 * <p>Set this attribute to {@code true} if the absence of invocations is
	 * expected in some cases and should not cause a test failure.
	 *
	 * <p>Defaults to {@code false}.
	 *
	 * @since 5.12
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	boolean allowZeroInvocations() default false;

	/**
	 * Configure how the number of arguments provided by an
	 * {@link ArgumentsSource} are validated.
	 *
	 * <p>Defaults to {@link ArgumentCountValidationMode#DEFAULT}.
	 *
	 * <p>When an {@link ArgumentsSource} provides more arguments than declared
	 * by the parameterized test method, there might be a bug in the method or
	 * the {@link ArgumentsSource}. By default, the additional arguments are
	 * ignored. {@code argumentCountValidation} allows you to control how
	 * additional arguments are handled. The default can be configured via the
	 * {@value ArgumentCountValidator#ARGUMENT_COUNT_VALIDATION_KEY}
	 * configuration parameter (see the User Guide for details on configuration
	 * parameters).
	 *
	 * @since 5.12
	 * @see ArgumentCountValidationMode
	 */
	@API(status = MAINTAINED, since = "5.13.3")
	ArgumentCountValidationMode argumentCountValidation() default ArgumentCountValidationMode.DEFAULT;

}
