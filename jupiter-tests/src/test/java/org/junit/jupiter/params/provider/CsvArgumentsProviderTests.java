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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.MockCsvAnnotationBuilder.csvSource;
import static org.junit.platform.commons.test.PreconditionAssertions.assertPreconditionViolationFor;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.assertj.core.api.ThrowingConsumer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.support.ParameterNameAndArgument;
import org.junit.platform.commons.JUnitException;

/**
 * @since 5.0
 */
class CsvArgumentsProviderTests {

	@Test
	void throwsExceptionForBlankLines() {
		var annotation = csvSource("foo", "bar", " ");

		assertThatExceptionOfType(JUnitException.class)//
				.isThrownBy(() -> provideArguments(annotation).toArray())//
				.withMessage("CSV record at index 3 must not be blank");
	}

	@Test
	void throwsExceptionIfNeitherValueNorTextBlockIsDeclared() {
		var annotation = csvSource().build();

		assertPreconditionViolationFor(() -> provideArguments(annotation).findAny())//
				.withMessage("@CsvSource must be declared with either `value` or `textBlock` but not both");
	}

	@Test
	void throwsExceptionIfValueAndTextBlockAreDeclared() {
		var annotation = csvSource().lines("foo").textBlock("""
				bar
				baz
				""").build();

		assertPreconditionViolationFor(() -> provideArguments(annotation).findAny())//
				.withMessage("@CsvSource must be declared with either `value` or `textBlock` but not both");
	}

	@Test
	void providesSingleArgument() {
		var annotation = csvSource("foo");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo"));
	}

	@Test
	void providesSingleArgumentFromTextBlock() {
		var annotation = csvSource().textBlock("foo").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo"));
	}

	@Test
	void providesMultipleArguments() {
		var annotation = csvSource("foo", "bar");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	@Test
	void providesMultipleArgumentsFromTextBlock() {
		var annotation = csvSource().textBlock("""
				foo
				bar
				""").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo"), array("bar"));
	}

	@Test
	void splitsAndTrimsArguments() {
		var annotation = csvSource(" foo , bar ");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo", "bar"));
	}

	/**
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/3824">GitHub issue #3824</a>
	 */
	@Test
	void trimsLeadingWhitespaceAndControlCharacters() {
		var annotation = csvSource("'', 1", "\t'',\b2", "'',\u00003", " '', \t 4");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("", "1"), array("", "2"), array("", "3"), array("", "4"));
	}

	/**
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/3824">GitHub issue #3824</a>
	 */
	@Test
	void trimsTrailingWhitespaceAndControlCharacters() {
		var annotation = csvSource("1 ,'' ", "2\t,''\b", "3   ,''\u0000", "4,'' \t ");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("1", ""), array("2", ""), array("3", ""), array("4", ""));
	}

	@Test
	void preservesLeadingAndTrailingWhitespaceAndControlCharactersWhenRequested() {
		var annotation = csvSource().lines(" 1 , a ", "\t2\b, b   ", "\u00003\u0007,c ", "4, \t d \t ") //
				.ignoreLeadingAndTrailingWhitespace(false).build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(//
			array(" 1 ", " a "), //
			array("\t2\b", " b   "), //
			array("\u00003\u0007", "c "), //
			array("4", " \t d \t "));
	}

	/**
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/3824">GitHub issue #3824</a>
	 */
	@Test
	void trimVsStripSemanticsWithUnquotedText() {
		// \u0000 (null character) removed by trim(), preserved by strip()
		// \u00A0 (non-breaking space) preserved by trim(), removed by strip()

		var annotation = csvSource().lines("\u0000, \u0000foo\u0000, \u00A0bar\u00A0").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("", "foo", "\u00A0bar\u00A0"));
	}

	/**
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/3824">GitHub issue #3824</a>
	 */
	@Test
	void trimVsStripSemanticsWithQuotedText() {
		// \u0000 (null character) removed by trim(), preserved by strip()
		// \u00A0 (non-breaking space) preserved by trim(), removed by strip()

		var annotation = csvSource().lines("'\u0000', '\u0000 foo \u0000', '\t\u00A0bar\u0000'").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("\u0000", "\u0000 foo \u0000", "\t\u00A0bar\u0000"));
	}

	/**
	 * @see <a href="https://github.com/junit-team/junit-framework/issues/3824">GitHub issue #3824</a>
	 */
	@Test
	void trimVsStripSemanticsWithUnquotedAndQuotedText() {
		// \u0000 (null character) removed by trim(), preserved by strip()
		// \u00A0 (non-breaking space) preserved by trim(), removed by strip()

		var annotation = csvSource().lines("\u0000'\u0000 foo', \u00A0' bar\u0000'").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("\u0000 foo", "\u00A0' bar\u0000'"));
	}

	@Test
	void understandsQuotes() {
		var annotation = csvSource("'foo, bar'");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo, bar"));
	}

	@Test
	void understandsQuotesInTextBlock() {
		var annotation = csvSource().textBlock("""
				'foo, bar'
				""").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo, bar"));
	}

	@Test
	void understandsCustomQuotes() {
		var annotation = csvSource().quoteCharacter('~').lines("~foo, bar~").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo, bar"));
	}

	@Test
	void understandsCustomQuotesInTextBlock() {
		var annotation = csvSource().quoteCharacter('"').textBlock("""
					"foo, bar"
				""").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo, bar"));
	}

	@Test
	void understandsEscapeCharacters() {
		var annotation = csvSource("'foo or ''bar''', baz");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo or 'bar'", "baz"));
	}

	@Test
	void understandsEscapeCharactersWithCustomQuoteCharacter() {
		var annotation = csvSource().quoteCharacter('~').lines("~foo or ~~bar~~~, baz").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo or ~bar~", "baz"));
	}

	@Test
	void doesNotTrimSpacesInsideQuotes() {
		var annotation = csvSource("''", "'   '", "'blank '", "' not blank   '");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array(""), array("   "), array("blank "), array(" not blank   "));
	}

	@Test
	void providesArgumentsWithCharacterDelimiter() {
		var annotation = csvSource().delimiter('|').lines("foo|bar", "bar|foo").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo", "bar"), array("bar", "foo"));
	}

	@Test
	void providesArgumentsWithStringDelimiter() {
		var annotation = csvSource().delimiterString("~~~").lines("foo~~~ bar", "bar~~~ foo").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("foo", "bar"), array("bar", "foo"));
	}

	@Test
	void throwsExceptionIfBothDelimitersAreSimultaneouslySet() {
		var annotation = csvSource().delimiter('|').delimiterString("~~~").build();

		assertPreconditionViolationFor(() -> provideArguments(annotation).findAny())//
				.withMessageStartingWith("The delimiter and delimiterString attributes cannot be set simultaneously in")//
				.withMessageContaining("CsvSource");
	}

	@Test
	void defaultEmptyValueAndDefaultNullValue() {
		var annotation = csvSource("'', null, ,, apple");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("", "null", null, null, "apple"));
	}

	@Test
	void customEmptyValueAndDefaultNullValue() {
		var annotation = csvSource().emptyValue("EMPTY").lines("'', null, ,, apple").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("EMPTY", "null", null, null, "apple"));
	}

	@Test
	void customNullValues() {
		var annotation = csvSource().nullValues("N/A", "NIL", "null")//
				.lines("apple, , NIL, '', N/A, banana, null").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("apple", null, null, "", null, "banana", null));
	}

	@Test
	void customNullValueInHeader() {
		var annotation = csvSource().useHeadersInDisplayName(true).nullValues("NIL").textBlock("""
				FRUIT, NIL
				apple, 1
				""").build();

		assertThat(headersToValues(annotation)).containsExactly(array("FRUIT = apple", "null = 1"));
	}

	@Test
	void convertsEmptyValuesToNullInLinesAfterFirstLine() {
		var annotation = csvSource("'', ''", " , ");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("", ""), array(null, null));
	}

	@Test
	void throwsExceptionIfSourceExceedsMaxCharsPerColumnConfig() {
		var annotation = csvSource().lines("413").maxCharsPerColumn(2).build();

		assertThatExceptionOfType(CsvParsingException.class)//
				.isThrownBy(() -> provideArguments(annotation).findAny())//
				.withMessageStartingWith("Failed to parse CSV input configured via Mock for CsvSource")//
				.havingRootCause().satisfies(isCsvParseException());
	}

	@Test
	void providesArgumentWithDefaultMaxCharsPerColumnConfig() {
		var annotation = csvSource().lines("0".repeat(4096)).delimiter(';').build();

		var arguments = provideArguments(annotation);

		assertThat(arguments.toArray()).hasSize(1);
	}

	@Test
	void throwsExceptionWhenSourceExceedsDefaultMaxCharsPerColumnConfig() {
		var annotation = csvSource().lines("0".repeat(4097)).delimiter(';').build();

		assertThatExceptionOfType(CsvParsingException.class)//
				.isThrownBy(() -> provideArguments(annotation).findAny())//
				.withMessageStartingWith("Failed to parse CSV input configured via Mock for CsvSource")//
				.havingRootCause().satisfies(isCsvParseException());
	}

	@Test
	void providesArgumentsForExceedsSourceWithCustomMaxCharsPerColumnConfig() {
		var annotation = csvSource().lines("0".repeat(4097)).maxCharsPerColumn(4097).build();

		var arguments = provideArguments(annotation);

		assertThat(arguments.toArray()).hasSize(1);
	}

	@ParameterizedTest
	@ValueSource(ints = { Integer.MIN_VALUE, -2, 0 })
	void throwsExceptionWhenMaxCharsPerColumnIsNotPositiveNumberOrMinusOne(int maxCharsPerColumn) {
		var annotation = csvSource().lines("41").maxCharsPerColumn(maxCharsPerColumn).build();

		assertPreconditionViolationFor(() -> provideArguments(annotation).findAny())//
				.withMessageStartingWith("maxCharsPerColumn must be a positive number or -1: " + maxCharsPerColumn);
	}

	@Test
	void ignoresCommentCharacterWhenUsingValueAttribute() {
		var annotation = csvSource("#foo", "#bar,baz", "baz,#quux");

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("#foo"), array("#bar", "baz"), array("baz", "#quux"));
	}

	@Test
	void honorsCommentCharacterWhenUsingTextBlockAttribute() {
		var annotation = csvSource().textBlock("""
				#foo
				bar, #baz
				'#bar', baz
				""").build();

		var arguments = provideArguments(annotation);

		assertThat(arguments).containsExactly(array("bar", "#baz"), array("#bar", "baz"));
	}

	@Test
	void supportsCsvHeadersWhenUsingTextBlockAttribute() {
		var annotation = csvSource().useHeadersInDisplayName(true).textBlock("""
				FRUIT, RANK
				apple, 1
				banana, 2
				""").build();

		assertThat(headersToValues(annotation)).containsExactly(//
			array("FRUIT = apple", "RANK = 1"), //
			array("FRUIT = banana", "RANK = 2")//
		);
	}

	@Test
	void supportsCsvHeadersWhenUsingValueAttribute() {
		var annotation = csvSource().useHeadersInDisplayName(true)//
				.lines("FRUIT, RANK", "apple, 1", "banana, 2").build();

		assertThat(headersToValues(annotation)).containsExactly(//
			array("FRUIT = apple", "RANK = 1"), //
			array("FRUIT = banana", "RANK = 2")//
		);
	}

	private Stream<String[]> headersToValues(CsvSource csvSource) {
		var arguments = provideArguments(csvSource);
		return arguments.map(array -> {
			String[] strings = new String[array.length];
			for (int i = 0; i < array.length; i++) {
				if (array[i] instanceof ParameterNameAndArgument parameterNameAndArgument) {
					strings[i] = parameterNameAndArgument.getName() + " = " + parameterNameAndArgument.getPayload();
				}
				else {
					throw new IllegalStateException("Unexpected argument type: " + array[i].getClass().getName());
				}
			}
			return strings;
		});
	}

	@Test
	void throwsExceptionIfColumnCountExceedsHeaderCount() {
		var annotation = csvSource().useHeadersInDisplayName(true).textBlock("""
				FRUIT, RANK
				apple, 1
				banana, 2, BOOM!
				""").build();

		assertPreconditionViolationFor(() -> provideArguments(annotation).findAny())//
				.withMessage(
					"The number of columns (3) exceeds the number of supplied headers (2) in CSV record: [banana, 2, BOOM!]");
	}

	private Stream<Object[]> provideArguments(CsvSource annotation) {
		var provider = new CsvArgumentsProvider();
		provider.accept(annotation);
		return provider.provideArguments(mock(), mock(ExtensionContext.class)).map(Arguments::get);
	}

	@SuppressWarnings("unchecked")
	private static <T> @Nullable T[] array(@Nullable T... elements) {
		return elements;
	}

	static ThrowingConsumer<Throwable> isCsvParseException() {
		return ex -> ex.getClass().getName().contains("de.siegmar.fastcsv.reader.CsvParseException");
	}

}
