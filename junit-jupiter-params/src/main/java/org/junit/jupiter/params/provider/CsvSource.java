/*
 * Copyright 2015-2026 the original author or authors.
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
 * {@code @CsvSource} is a {@linkplain Repeatable repeatable}
 * {@link ArgumentsSource} which reads comma-separated values (CSV) from one
 * or more CSV records supplied via the {@link #value} attribute or
 * {@link #textBlock} attribute.
 *
 * <p>The supplied values will be provided as arguments to the annotated
 * {@link org.junit.jupiter.params.ParameterizedClass @ParameterizedClass} or
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}.
 *
 * <p>The column delimiter (which defaults to a comma ({@code ,})) can be customized
 * via either {@link #delimiter} or {@link #delimiterString}.
 *
 * <p>By default, {@code @CsvSource} uses a single quote ({@code '}) as its quote
 * character, but this can be changed via {@link #quoteCharacter}. See the
 * {@code 'lemon, lime'} examples in the documentation for the {@link #value}
 * and {@link #textBlock} attributes. An empty, quoted value ({@code ''}) results
 * in an empty {@link String} unless the {@link #emptyValue} attribute is set;
 * whereas, an entirely <em>empty</em> value is interpreted as a {@code null} reference.
 * By specifying one or more {@link #nullValues} a custom value can be interpreted
 * as a {@code null} reference (see the User Guide for an example). An
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
 * <p>In general, CSV records should not contain explicit newlines ({@code \n})
 * unless they are placed within quoted strings. Note that CSV records supplied
 * via {@link #textBlock} will implicitly contain newlines at the end of each
 * physical line within the text block. Thus, if a CSV column wraps across a
 * new line in a text block, the column must be a quoted string.
 *
 * <p>Note that {@link #delimiter} (or {@link #delimiterString}),
 * {@link #quoteCharacter}, and {@link #commentCharacter} (when
 * {@link #textBlock} is used) are treated as <em>control characters</em>.
 *
 * <ul>
 *   <li>{@link #delimiter} and {@link #quoteCharacter} must always be distinct.</li>
 *   <li>{@link #commentCharacter} must be distinct from the others only when
 *   {@link #textBlock} is used.</li>
 * </ul>
 *
 * <h2>Inheritance</h2>
 *
 * <p>This annotation is {@linkplain Inherited inherited} within class hierarchies.
 *
 * @since 5.0
 * @see CsvFileSource
 * @see org.junit.jupiter.params.provider.ArgumentsSource
 * @see org.junit.jupiter.params.ParameterizedClass
 * @see org.junit.jupiter.params.ParameterizedTest
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CsvSources.class)
@Documented
@Inherited
@API(status = STABLE, since = "5.7")
@ArgumentsSource(CsvArgumentsProvider.class)
@SuppressWarnings("exports")
public @interface CsvSource {

	/**
	 * The CSV records to use as the source of arguments; must not be empty.
	 *
	 * <p>Defaults to an empty array. You therefore must supply CSV content
	 * via this attribute or the {@link #textBlock} attribute.
	 *
	 * <p>Each value corresponds to a record in a CSV file and will be split using
	 * the specified {@link #delimiter} or {@link #delimiterString}. Note that
	 * the first value may optionally be used to supply CSV headers (see
	 * {@link #useHeadersInDisplayName}). Moreover, each specified value must
	 * not be blank.
	 *
	 * <p>If <em>text block</em> syntax is supported by your programming language,
	 * you may find it more convenient to declare your CSV content via the
	 * {@link #textBlock} attribute.
	 *
	 * <h4>Example</h4>
	 * <pre class="code">
	 * {@literal @}ParameterizedTest
	 * {@literal @}CsvSource({
	 *     "apple,         1",
	 *     "banana,        2",
	 *     "'lemon, lime', 0xF1",
	 *     "strawberry,    700_000"
	 * })
	 * void test(String fruit, int rank) {
	 *     // ...
	 * }</pre>
	 *
	 * @see #textBlock
	 */
	String[] value() default {};

	/**
	 * The CSV records to use as the source of arguments, supplied as a single
	 * <em>text block</em>; must not be empty.
	 *
	 * <p>Defaults to an empty string. You therefore must supply CSV content
	 * via this attribute or the {@link #value} attribute.
	 *
	 * <p>Text block syntax is supported by various languages on the JVM
	 * including Java SE. If text blocks are not supported, you
	 * should declare your CSV content via the {@link #value} attribute.
	 *
	 * <p>Each record in the text block corresponds to a record in a CSV file and will
	 * be split using the specified {@link #delimiter} or {@link #delimiterString}.
	 * Note that the first record may optionally be used to supply CSV headers (see
	 * {@link #useHeadersInDisplayName}).
	 *
	 * <p>In contrast to CSV records supplied via {@link #value}, a text block
	 * can contain comments. Any line beginning with a {@link #commentCharacter}
	 * will be treated as a comment and ignored. Note that there is one exception
	 * to this rule: if the comment character appears within a quoted field,
	 * it loses its special meaning.
	 *
	 * <p>The comment character must be the first character on the line without
	 * any leading whitespace. It is therefore recommended that the closing text block
	 * delimiter {@code """} be placed either at the end of the last line of
	 * input or on the following line, vertically aligned with the rest of the
	 * input (as can be seen in the example below).
	 *
	 * <p>Java's <a href="https://docs.oracle.com/en/java/javase/17/text-blocks/index.html">text block</a>
	 * feature automatically removes <em>incidental whitespace</em> when the code
	 * is compiled. However, other JVM languages such as Groovy and Kotlin do not.
	 * Thus, if you are using a programming language other than Java and your text
	 * block contains comments or new lines within quoted strings, you will need
	 * to ensure that there is no leading whitespace within your text block.
	 *
	 * <h4>Example</h4>
	 * <pre class="code">
	 * {@literal @}ParameterizedTest
	 * {@literal @}CsvSource(quoteCharacter = '"', textBlock = """
	 *     # FRUIT,       RANK
	 *     apple,         1
	 *     banana,        2
	 *     "lemon, lime", 0xF1
	 *     strawberry,    700_000
	 *     """)
	 * void test(String fruit, int rank) {
	 *     // ...
	 * }</pre>
	 *
	 * @since 5.8.1
	 * @see #value
	 * @see #quoteCharacter
	 */
	@API(status = STABLE, since = "5.10")
	String textBlock() default "";

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
	 * {@literal @}CsvSource(useHeadersInDisplayName = true, textBlock = """
	 *     FRUIT,         RANK
	 *     apple,         1
	 *     banana,        2
	 *     'lemon, lime', 0xF1
	 *     strawberry,    700_000
	 *     """)
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
	 * <p>Defaults to a single quote ({@code '}).
	 *
	 * <p>You may change the quote character to anything that makes sense for
	 * your use case; however, the primary use case is to allow you to use double
	 * quotes in {@link #textBlock}.
	 *
	 * @since 5.8.2
	 * @see #textBlock
	 */
	@API(status = STABLE, since = "5.10")
	char quoteCharacter() default '\'';

	/**
	 * The column delimiter character to use when reading the {@linkplain #value records}.
	 *
	 * <p>This is an alternative to {@link #delimiterString} and cannot be
	 * used in conjunction with {@link #delimiterString}.
	 *
	 * <p>Defaults implicitly to {@code ','}, if neither delimiter attribute is
	 * explicitly set.
	 */
	char delimiter() default '\0';

	/**
	 * The column delimiter string to use when reading the {@linkplain #value records}.
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
	 * The empty value to use when reading the {@linkplain #value records}.
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
	 * The character used to denote comments in a {@linkplain #textBlock text block}.
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
