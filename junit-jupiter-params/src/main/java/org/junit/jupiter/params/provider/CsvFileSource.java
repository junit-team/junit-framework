/*
 * Copyright 2015-2025 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.params.ParameterizedInvocationConstants;

/**
 * {@code @CsvFileSource} is a {@linkplain Repeatable repeatable}
 * {@link ArgumentsSource} which is used to load comma-separated value (CSV)
 * files from one or more classpath {@link #resources} or {@link #files}.
 *
 * <p>The CSV records parsed from these resources and files will be provided as
 * arguments to the annotated
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass} or
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}. Note
 * that the first record may optionally be used to supply CSV headers (see
 * {@link #useHeadersInDisplayName}).
 *
 * <p>Any line beginning with a {@link #commentCharacter}
 * will be interpreted as a comment and will be ignored.
 *
 * <p>The column delimiter (which defaults to a comma ({@code ,})) can be customized
 * via either {@link #delimiter} or {@link #delimiterString}.
 *
 * <p>The line separator is detected automatically, meaning that any of
 * {@code "\r"}, {@code "\n"}, or {@code "\r\n"} is treated as a line separator.
 *
 * <p>In contrast to the default syntax used in {@code @CsvSource}, {@code @CsvFileSource}
 * uses a double quote ({@code "}) as its quote character by default, but this can
 * be changed via {@link #quoteCharacter}. An empty, quoted value ({@code ""})
 * results in an empty {@link String} unless the {@link #emptyValue} attribute is
 * set; whereas, an entirely <em>empty</em> value is interpreted as a {@code null}
 * reference. By specifying one or more {@link #nullValues} a custom value can be
 * interpreted as a {@code null} reference (see the User Guide for an example). An
 * {@link org.junit.jupiter.params.converter.ArgumentConversionException
 * ArgumentConversionException} is thrown if the target type of a {@code null}
 * reference is a primitive type.
 *
 * <p>NOTE: An <em>unquoted</em> empty value will always be converted to a
 * {@code null} reference regardless of any custom values configured via the
 * {@link #nullValues} attribute.
 *
 * <p>Except within a quoted string, leading and trailing whitespace in a CSV
 * column is trimmed by default. This behavior can be changed by setting the
 * {@link #ignoreLeadingAndTrailingWhitespace} attribute to {@code true}.
 *
 * <p>Note that {@link #delimiter} (or {@link #delimiterString}),
 * {@link #quoteCharacter}, and {@link #commentCharacter} are treated as
 * <em>control characters</em> and must all be distinct.
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.0
 * @see CsvSource
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.ParameterizedClass
 * @see org.junit.jupiter.params.ParameterizedTest
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(CsvFileSources.class)
@API(status = STABLE, since = "5.7")
@ArgumentsSource(CsvFileArgumentsProvider.class)
@SuppressWarnings("exports")
public @interface CsvFileSource {

	/**
	 * The CSV classpath resources to use as the sources of arguments; must not
	 * be empty unless {@link #files} is non-empty.
	 */
	String[] resources() default {};

	/**
	 * The CSV files to use as the sources of arguments; must not be empty
	 * unless {@link #resources} is non-empty.
	 */
	String[] files() default {};

	/**
	 * The encoding to use when reading the CSV files; must be a valid charset.
	 *
	 * <p>Defaults to {@code "UTF-8"}.
	 *
	 * @see java.nio.charset.StandardCharsets
	 */
	String encoding() default "UTF-8";

	/**
	 * Configures whether the first CSV record should be treated as header names
	 * for columns.
	 *
	 * <p>When set to {@code true}, the header names will be used in the
	 * generated display name for each {@code @ParameterizedClass} or
	 * {@code @ParameterizedTest} invocation. When using this feature, you must
	 * ensure that the display name pattern for {@code @ParameterizedClass} or
	 * {@code @ParameterizedTest} includes
	 * {@value ParameterizedInvocationConstants#ARGUMENTS_PLACEHOLDER} instead of
	 * {@value ParameterizedInvocationConstants#ARGUMENTS_WITH_NAMES_PLACEHOLDER}
	 * as demonstrated in the example below.
	 *
	 * <p>Defaults to {@code false}.
	 *
	 * <h4>Example</h4>
	 * <pre class="code">
	 * {@literal @}ParameterizedTest(name = "[{index}] {arguments}")
	 * {@literal @}CsvFileSource(resources = "fruits.csv", useHeadersInDisplayName = true)
	 * void test(String fruit, int rank) {
	 *     // ...
	 * }</pre>
	 *
	 * @since 5.8.2
	 */
	@API(status = STABLE, since = "5.10")
	boolean useHeadersInDisplayName() default false;

	/**
	 * The quote character to use for <em>quoted strings</em>.
	 *
	 * <p>Defaults to a double quote ({@code "}).
	 *
	 * <p>You may change the quote character to anything that makes sense for
	 * your use case.
	 *
	 * @since 5.8.2
	 */
	@API(status = STABLE, since = "5.10")
	char quoteCharacter() default '"';

	/**
	 * The column delimiter character to use when reading the CSV files.
	 *
	 * <p>This is an alternative to {@link #delimiterString} and cannot be
	 * used in conjunction with {@link #delimiterString}.
	 *
	 * <p>Defaults implicitly to {@code ','}, if neither delimiter attribute is
	 * explicitly set.
	 */
	char delimiter() default '\0';

	/**
	 * The column delimiter string to use when reading the CSV files.
	 *
	 * <p>This is an alternative to {@link #delimiter} and cannot be used in
	 * conjunction with {@link #delimiter}.
	 *
	 * <p>Defaults implicitly to {@code ","}, if neither delimiter attribute is
	 * explicitly set.
	 *
	 * @since 5.6
	 */
	String delimiterString() default "";

	/**
	 * The number of lines to skip when reading the CSV files.
	 *
	 * <p>Typically used to skip header lines.
	 *
	 * <p>Defaults to {@code 0}.
	 *
	 * @since 5.1
	 */
	int numLinesToSkip() default 0;

	/**
	 * The empty value to use when reading the CSV files.
	 *
	 * <p>This value replaces quoted empty strings read from the input.
	 *
	 * <p>Defaults to {@code ""}.
	 *
	 * @since 5.5
	 */
	String emptyValue() default "";

	/**
	 * A list of strings that should be interpreted as {@code null} references.
	 *
	 * <p>For example, you may wish for certain values such as {@code "N/A"} or
	 * {@code "NIL"} to be converted to {@code null} references.
	 *
	 * <p>Please note that <em>unquoted</em> empty values will always be converted
	 * to {@code null} references regardless of the value of this {@code nullValues}
	 * attribute; whereas, a <em>quoted</em> empty string will be treated as an
	 * {@link #emptyValue}.
	 *
	 * <p>Defaults to {@code {}}.
	 *
	 * @since 5.6
	 */
	String[] nullValues() default {};

	/**
	 * The maximum number of characters allowed per CSV column.
	 *
	 * <p>Must be a positive number or {@code -1} to allow an unlimited number
	 * of characters.
	 *
	 * <p>Defaults to {@code 4096}.
	 *
	 * @since 5.7
	 */
	@API(status = STABLE, since = "5.10")
	int maxCharsPerColumn() default 4096;

	/**
	 * Controls whether leading and trailing whitespace characters of unquoted
	 * CSV columns should be ignored.
	 *
	 * <p>Whitespace refers to characters with Unicode code points less than
	 * or equal to {@code U+0020}, as defined by {@link String#trim()}.
	 *
	 * <p>Defaults to {@code true}.
	 *
	 * @since 5.8
	 */
	@API(status = STABLE, since = "5.10")
	boolean ignoreLeadingAndTrailingWhitespace() default true;

	/**
	 * The character used to denote comments when reading the CSV files.
	 *
	 * <p>Any line that begins with this character will be treated as a comment
	 * and ignored during parsing. Note that there is one exception to this rule:
	 * if the comment character appears within a quoted field, it loses its
	 * special meaning.
	 *
	 * <p>The comment character must be the first character on the line without
	 * any leading whitespace.
	 *
	 * <p>Defaults to {@code '#'}.
	 *
	 * @since 6.0.1
	 */
	@API(status = EXPERIMENTAL, since = "6.0.1")
	char commentCharacter() default '#';

}
