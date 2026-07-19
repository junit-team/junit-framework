/*
 * Copyright 2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Stream;

import de.siegmar.fastcsv.reader.CommentStrategy;

import org.junit.platform.commons.util.Preconditions;

/**
 * Wrapper for {@link CsvSource} with {@code value}, {@link CsvSource} with a
 * {@code textBlock} and {@link CsvFileSource}.
 */
record CsvReaderConfiguration( //
		CommentStrategy commentStrategy, //
		boolean namedCsvRecords, //
		char commentCharacter, //
		char delimiter, //
		String delimiterString, //
		String emptyValue, //
		boolean ignoreLeadingAndTrailingWhitespace, //
		int maxCharsPerColumn, //
		Set<String> nullValues, //
		char quoteCharacter //
) {

	private static final String DEFAULT_DELIMITER = ",";
	private static final char EMPTY_CHAR = '\0';

	static CsvReaderConfiguration fromValueCsvSource(CsvSource csvSource) {
		validateValueAndTextBlock(csvSource);
		Preconditions.condition(csvSource.value().length > 0, () -> "@CsvSource must be declared with `value`");
		return validate(csvSource, new CsvReaderConfiguration( //
			// CsvSource.value does not support comments
			CommentStrategy.NONE,
			// For CsvSource.value we manually process the header.
			false, //
			csvSource.commentCharacter(), //
			csvSource.delimiter(), //
			csvSource.delimiterString(), //
			csvSource.emptyValue(), //
			csvSource.ignoreLeadingAndTrailingWhitespace(), //
			csvSource.maxCharsPerColumn(), //
			Set.of(csvSource.nullValues()), //
			csvSource.quoteCharacter()//
		));
	}

	static CsvReaderConfiguration fromTextBlockCsvSource(CsvSource csvSource) {
		validateValueAndTextBlock(csvSource);
		Preconditions.condition(!csvSource.textBlock().isEmpty(),
			() -> "@CsvSource must be declared with a `textBlock`");
		return validate(csvSource, new CsvReaderConfiguration( //
			CommentStrategy.SKIP, csvSource.useHeadersInDisplayName(), csvSource.commentCharacter(), //
			csvSource.delimiter(), //
			csvSource.delimiterString(), //
			csvSource.emptyValue(), //
			csvSource.ignoreLeadingAndTrailingWhitespace(), //
			csvSource.maxCharsPerColumn(), //
			Set.of(csvSource.nullValues()), //
			csvSource.quoteCharacter()//
		));
	}

	static CsvReaderConfiguration fromCsvFileSource(CsvFileSource csvSource) {
		return validate(csvSource, new CsvReaderConfiguration( //
			CommentStrategy.SKIP, csvSource.useHeadersInDisplayName(), csvSource.commentCharacter(), //
			csvSource.delimiter(), //
			csvSource.delimiterString(), //
			csvSource.emptyValue(), //
			csvSource.ignoreLeadingAndTrailingWhitespace(), //
			csvSource.maxCharsPerColumn(), //
			Set.of(csvSource.nullValues()), //
			csvSource.quoteCharacter()//
		));
	}

	private static void validateValueAndTextBlock(CsvSource csvSource) {
		var values = csvSource.value();
		Preconditions.condition(values.length > 0 ^ !csvSource.textBlock().isEmpty(),
			() -> "@CsvSource must be declared with either `value` or `textBlock` but not both");
		for (int i = 0; i < values.length; i++) {
			int finalI = i;
			Preconditions.notBlank(values[i], () -> "CSV record at index %d must not be blank".formatted(finalI + 1));
		}
	}

	private static CsvReaderConfiguration validate(Annotation csvSource, CsvReaderConfiguration configuration) {
		validateMaxCharsPerColumn(configuration.maxCharsPerColumn());
		validateDelimiter( //
			configuration.delimiter(), //
			configuration.delimiterString(), //
			csvSource //
		);
		validateControlCharactersDiffer( //
			configuration.fieldSeparator(), //
			configuration.quoteCharacter(), //
			configuration.commentCharacter(), //
			configuration.commentStrategy() //
		);
		return configuration;
	}

	private static void validateMaxCharsPerColumn(int maxCharsPerColumn) {
		Preconditions.condition(maxCharsPerColumn > 0 || maxCharsPerColumn == -1,
			() -> "maxCharsPerColumn must be a positive number or -1: " + maxCharsPerColumn);
	}

	private static void validateDelimiter(char delimiter, String delimiterString, Annotation annotation) {
		Preconditions.condition(delimiter == EMPTY_CHAR || delimiterString.isEmpty(),
			() -> "The delimiter and delimiterString attributes cannot be set simultaneously in " + annotation);
	}

	private static void validateControlCharactersDiffer(String fieldSeparator, char quoteCharacter,
			char commentCharacter, CommentStrategy commentStrategy) {

		if (commentStrategy == CommentStrategy.NONE) {
			Preconditions.condition(stringValuesUnique(fieldSeparator, quoteCharacter),
				() -> ("delimiter or delimiterString: '%s' and quoteCharacter: '%s' " + //
						"must differ").formatted(fieldSeparator, quoteCharacter));
		}
		else {
			Preconditions.condition(stringValuesUnique(fieldSeparator, quoteCharacter, commentCharacter),
				() -> ("delimiter or delimiterString: '%s', quoteCharacter: '%s', and commentCharacter: '%s' " + //
						"must all differ").formatted(fieldSeparator, quoteCharacter, commentCharacter));
		}
	}

	private static boolean stringValuesUnique(Object... values) {
		long uniqueCount = Stream.of(values).map(String::valueOf).distinct().count();
		return uniqueCount == values.length;
	}

	int maxFieldSize() {
		return maxCharsPerColumn() == -1 ? Integer.MAX_VALUE : maxCharsPerColumn();
	}

	String fieldSeparator() {
		char delimiter = delimiter();
		if (delimiter != EMPTY_CHAR) {
			return String.valueOf(delimiter);
		}
		var delimiterString = delimiterString();
		if (!delimiterString.isEmpty()) {
			return delimiterString;
		}
		return DEFAULT_DELIMITER;
	}

}
