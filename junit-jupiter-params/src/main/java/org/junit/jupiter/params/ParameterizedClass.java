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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * {@code @ParameterizedClass} is used to signal that the annotated class is
 * a <em>parameterized test class</em>.
 *
 * <h2>Arguments Providers and Sources</h2>
 *
 * <p>A {@code @ParameterizedClass} must specify at least one
 * {@link org.junit.jupiter.params.provider.ArgumentsProvider ArgumentsProvider}
 * via {@link org.junit.jupiter.params.provider.ArgumentsSource @ArgumentsSource}
 * or a corresponding composed annotation (such as {@code @ValueSource},
 * {@code @CsvSource}, etc.). The provider is responsible for providing a
 * {@link java.util.stream.Stream Stream} of
 * {@link org.junit.jupiter.params.provider.Arguments Arguments} that will be
 * used to invoke the parameterized class.
 *
 * <h2>Field or Constructor Injection</h2>
 *
 * <p>The provided arguments can either be injected into fields annotated with
 * {@link Parameter @Parameter} or passed to the unique constructor of the
 * parameterized class. If a {@code @Parameter}-annotated field is declared in
 * the parameterized class or one of its superclasses, field injection will be
 * used. Otherwise, constructor injection will be used.
 *
 * <h3>Constructor Injection</h3>
 *
 * <p>A {@code @ParameterizedClass} constructor may declare additional
 * parameters at the end of its parameter list to be resolved by other
 * {@link org.junit.jupiter.api.extension.ParameterResolver ParameterResolvers}
 * (such as {@code TestInfo}, {@code TestReporter}, etc.). Specifically, such a
 * constructor must declare formal parameters according to the following rules.
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
 * is supplied as an argument to the parameterized class at the same index in
 * the constructor's formal parameter list. An <em>aggregator</em> is any
 * parameter of type
 * {@link org.junit.jupiter.params.aggregator.ArgumentsAccessor ArgumentsAccessor}
 * or any parameter annotated with
 * {@link org.junit.jupiter.params.aggregator.AggregateWith @AggregateWith}.
 *
 * <h3>Field injection</h3>
 *
 * <p>Fields annotated with {@code @Parameter} must be declared according to the
 * following rules.
 *
 * <ol>
 * <li>Zero or more <em>indexed parameters</em> may be declared; each must have
 * a unique index specified in its {@code @Parameter(index)} annotation. The
 * index may be omitted if there is only one indexed parameter. If there are at
 * least two indexed parameter declarations, there must be declarations for all
 * indexes from 0 to the largest declared index.</li>
 * <li>Zero or more <em>aggregators</em> may be declared; each without
 * specifying an index in its {@code @Parameter} annotation.</li>
 * <li>Zero or more other fields may be declared as usual as long as they're not
 * annotated with {@code @Parameter}.</li>
 * </ol>
 *
 * <p>In this context, an <em>indexed parameter</em> is an argument for a given
 * index in the {@code Arguments} provided by an {@code ArgumentsProvider} that
 * is injected into a field annotated with {@code @Parameter(index)}. An
 * <em>aggregator</em> is any {@code @Parameter}-annotated field of type
 * {@link org.junit.jupiter.params.aggregator.ArgumentsAccessor ArgumentsAccessor}
 * or any field annotated with
 * {@link org.junit.jupiter.params.aggregator.AggregateWith @AggregateWith}.
 *
 * <h2>Argument Conversion</h2>
 *
 * <p>{@code @Parameter}-annotated fields or constructor parameters may be
 * annotated with
 * {@link org.junit.jupiter.params.converter.ConvertWith @ConvertWith}
 * or a corresponding composed annotation to specify an <em>explicit</em>
 * {@link org.junit.jupiter.params.converter.ArgumentConverter ArgumentConverter}.
 * Otherwise, JUnit Jupiter will attempt to perform an <em>implicit</em>
 * conversion to the target type automatically (see the User Guide for further
 * details).
 *
 * <h2>Lifecycle Methods</h2>
 *
 * <p>If you wish to execute custom code before or after each invocation of the
 * parameterized class, you may declare methods annotated with
 * {@link BeforeParameterizedClassInvocation @BeforeParameterizedClassInvocation}
 * or {@link AfterParameterizedClassInvocation @AfterParameterizedClassInvocation}.
 * This can, for example, be useful to initialize the arguments before they are
 * used.
 *
 * <h2>Composed Annotations</h2>
 *
 * <p>{@code @ParameterizedClass} may also be used as a meta-annotation in
 * order to create a custom <em>composed annotation</em> that inherits the
 * semantics of {@code @ParameterizedClass}.
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.13
 * @see Parameter
 * @see BeforeParameterizedClassInvocation
 * @see AfterParameterizedClassInvocation
 * @see ParameterizedTest
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
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@API(status = EXPERIMENTAL, since = "6.0")
@ClassTemplate
@ExtendWith(ParameterizedClassExtension.class)
@SuppressWarnings("exports")
public @interface ParameterizedClass {

	/**
	 * The display name to be used for individual invocations of the
	 * parameterized class; never blank or consisting solely of whitespace.
	 *
	 * <p>Defaults to
	 * <code>{@value ParameterizedInvocationNameFormatter#DEFAULT_DISPLAY_NAME}</code>.
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
	 * <li>Otherwise,
	 * <code>{@value ParameterizedInvocationConstants#DEFAULT_DISPLAY_NAME}</code>
	 * will be used.</li>
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
	 * performed. For example, if a parameterized class accepts {@code 3.14} as a
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
	 * Configure whether all arguments of the parameterized class that implement
	 * {@link AutoCloseable} will be closed after their corresponding
	 * invocation.
	 *
	 * <p>Defaults to {@code true}.
	 *
	 * <p><strong>WARNING</strong>: if an argument that implements
	 * {@code AutoCloseable} is reused for multiple invocations of the same
	 * parameterized class, you must set {@code autoCloseArguments} to
	 * {@code false} to ensure that the argument is not closed between
	 * invocations.
	 *
	 * @see java.lang.AutoCloseable
	 */
	boolean autoCloseArguments() default true;

	/**
	 * Configure whether zero invocations are allowed for this
	 * parameterized class.
	 *
	 * <p>Set this attribute to {@code true} if the absence of invocations is
	 * expected in some cases and should not cause a test failure.
	 *
	 * <p>Defaults to {@code false}.
	 */
	boolean allowZeroInvocations() default false;

	/**
	 * Configure how the number of arguments provided by an
	 * {@link ArgumentsSource} are validated.
	 *
	 * <p>Defaults to {@link ArgumentCountValidationMode#DEFAULT}.
	 *
	 * <p>When an {@link ArgumentsSource} provides more arguments than declared
	 * by the parameterized class constructor or {@link Parameter}-annotated
	 * fields, there might be a bug in the parameterized class or the
	 * {@link ArgumentsSource}. By default, the additional arguments are
	 * ignored. {@code argumentCountValidation} allows you to control how
	 * additional arguments are handled. The default can be configured via the
	 * {@value ArgumentCountValidator#ARGUMENT_COUNT_VALIDATION_KEY}
	 * configuration parameter (see the User Guide for details on configuration
	 * parameters).
	 *
	 * @see ArgumentCountValidationMode
	 */
	ArgumentCountValidationMode argumentCountValidation() default ArgumentCountValidationMode.DEFAULT;

}
